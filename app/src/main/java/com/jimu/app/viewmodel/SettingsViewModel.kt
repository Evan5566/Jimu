package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jimu.app.data.backup.BackupFileReadResult
import com.jimu.app.data.backup.BackupFileWriteResult
import com.jimu.app.data.backup.BackupImportPreview
import com.jimu.app.data.backup.BackupImportPreviewResult
import com.jimu.app.data.backup.ReminderRebuildResult
import com.jimu.app.data.backup.RestoreDatabaseResult
import com.jimu.app.data.backup.ValidatedBackupPayload
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface SettingsUiState {
    data object Idle : SettingsUiState
    data object Exporting : SettingsUiState

    data class WaitingForExportLocation(
        val json: String,
        val fileName: String
    ) : SettingsUiState

    data object WritingExport : SettingsUiState
    data object ReadingImportFile : SettingsUiState

    data class PreviewReady(
        val preview: BackupImportPreview,
        val validated: ValidatedBackupPayload
    ) : SettingsUiState

    data object PreparingPreImportBackup : SettingsUiState

    data class WaitingForPreImportBackupLocation(
        val validated: ValidatedBackupPayload,
        val json: String,
        val fileName: String
    ) : SettingsUiState

    data class SavingPreImportBackup(
        val validated: ValidatedBackupPayload
    ) : SettingsUiState

    data object RestoringDatabase : SettingsUiState
    data object RebuildingReminders : SettingsUiState
    data class Success(val message: String) : SettingsUiState
    data class PartialSuccess(val message: String) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

class SettingsViewModel(
    private val createExportJson: suspend () -> String,
    private val decodeAndValidate: (String) -> BackupImportPreviewResult,
    private val restoreValidatedPayload: suspend (ValidatedBackupPayload) -> RestoreDatabaseResult,
    private val rebuildReminders: suspend (List<Long>) -> ReminderRebuildResult,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val timestampProvider: () -> String
) : ViewModel() {
    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun requestExport() {
        _uiState.value = SettingsUiState.Exporting
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) { createExportJson() }
            }.onSuccess { json ->
                _uiState.value = SettingsUiState.WaitingForExportLocation(
                    json = json,
                    fileName = "jimu-backup-${timestampProvider()}.json"
                )
            }.onFailure { error ->
                _uiState.value = SettingsUiState.Error(
                    error.message ?: "生成备份失败"
                )
            }
        }
    }

    fun onExportLocationCancelled() {
        _uiState.value = SettingsUiState.Idle
    }

    fun onExportLocationSelected() {
        _uiState.value = SettingsUiState.WritingExport
    }

    fun onExportWritten(result: BackupFileWriteResult) {
        _uiState.value = when (result) {
            BackupFileWriteResult.Success -> SettingsUiState.Success("备份已导出")
            is BackupFileWriteResult.FileTooLarge -> {
                SettingsUiState.Error("备份内容超过 10 MiB，无法导出")
            }
            is BackupFileWriteResult.WriteFailed -> SettingsUiState.Error(
                result.message.ifBlank { "写入备份文件失败" }
            )
        }
    }

    fun onImportPickerOpened() {
        _uiState.value = SettingsUiState.ReadingImportFile
    }

    fun onImportPickerCancelled() {
        _uiState.value = SettingsUiState.Idle
    }

    fun onImportFileRead(result: BackupFileReadResult) {
        when (result) {
            is BackupFileReadResult.Success -> decodeImport(result.content)
            is BackupFileReadResult.FileTooLarge -> {
                _uiState.value = SettingsUiState.Error("备份文件超过 10 MiB 上限")
            }
            BackupFileReadResult.InvalidEncoding -> {
                _uiState.value = SettingsUiState.Error("备份文件不是有效的 UTF-8 文本")
            }
            is BackupFileReadResult.ReadFailed -> {
                _uiState.value = SettingsUiState.Error(result.message)
            }
        }
    }

    fun confirmRestore() {
        val previewState = _uiState.value as? SettingsUiState.PreviewReady ?: return
        _uiState.value = SettingsUiState.PreparingPreImportBackup
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) { createExportJson() }
            }.onSuccess { json ->
                _uiState.value = SettingsUiState.WaitingForPreImportBackupLocation(
                    validated = previewState.validated,
                    json = json,
                    fileName = "jimu-pre-restore-${timestampProvider()}.json"
                )
            }.onFailure { error ->
                _uiState.value = SettingsUiState.Error(
                    error.message ?: "生成恢复前保险备份失败"
                )
            }
        }
    }

    fun cancelRestorePreview() {
        _uiState.value = SettingsUiState.Idle
    }

    fun onPreImportBackupLocationCancelled() {
        _uiState.value = SettingsUiState.Idle
    }

    fun onPreImportBackupLocationSelected() {
        val waiting = _uiState.value as? SettingsUiState.WaitingForPreImportBackupLocation
            ?: return
        _uiState.value = SettingsUiState.SavingPreImportBackup(waiting.validated)
    }

    fun onPreImportBackupWritten(result: BackupFileWriteResult) {
        val saving = _uiState.value as? SettingsUiState.SavingPreImportBackup ?: return
        when (result) {
            BackupFileWriteResult.Success -> restoreDatabase(saving.validated)
            is BackupFileWriteResult.FileTooLarge -> {
                _uiState.value = SettingsUiState.Error("恢复前保险备份超过 10 MiB")
            }
            is BackupFileWriteResult.WriteFailed -> {
                _uiState.value = SettingsUiState.Error(result.message)
            }
        }
    }

    fun clearResult() {
        _uiState.value = SettingsUiState.Idle
    }

    private fun decodeImport(json: String) {
        _uiState.value = SettingsUiState.ReadingImportFile
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                decodeAndValidate(json)
            }
            _uiState.value = when (result) {
                is BackupImportPreviewResult.ReadyToConfirm -> {
                    SettingsUiState.PreviewReady(
                        preview = result.preview,
                        validated = result.validated
                    )
                }
                is BackupImportPreviewResult.InvalidFormat -> {
                    SettingsUiState.Error("备份文件格式错误：${result.message}")
                }
                is BackupImportPreviewResult.UnsupportedVersion -> {
                    SettingsUiState.Error("不支持的备份版本：${result.version}")
                }
                is BackupImportPreviewResult.ValidationFailed -> {
                    SettingsUiState.Error("备份校验失败：${result.message}")
                }
            }
        }
    }

    private fun restoreDatabase(validated: ValidatedBackupPayload) {
        _uiState.value = SettingsUiState.RestoringDatabase
        viewModelScope.launch {
            val restoreResult = withContext(ioDispatcher) {
                restoreValidatedPayload(validated)
            }
            when (restoreResult) {
                is RestoreDatabaseResult.RestoreFailed -> {
                    _uiState.value = SettingsUiState.Error(restoreResult.message)
                }
                is RestoreDatabaseResult.Success -> {
                    _uiState.value = SettingsUiState.RebuildingReminders
                    val reminderResult = withContext(ioDispatcher) {
                        rebuildReminders(restoreResult.oldTaskIds)
                    }
                    _uiState.value = when (reminderResult) {
                        ReminderRebuildResult.Success -> {
                            SettingsUiState.Success("数据恢复完成")
                        }
                        is ReminderRebuildResult.PartialFailure -> {
                            SettingsUiState.PartialSuccess(
                                "数据恢复成功，但部分提醒重建失败"
                            )
                        }
                        is ReminderRebuildResult.Failed -> {
                            SettingsUiState.PartialSuccess(
                                "数据恢复成功，但提醒重建失败"
                            )
                        }
                    }
                }
            }
        }
    }
}
