package com.jimu.app.viewmodel

import com.jimu.app.data.backup.BackupFileReadResult
import com.jimu.app.data.backup.BackupFileWriteResult
import com.jimu.app.data.backup.BackupImportPreview
import com.jimu.app.data.backup.BackupImportPreviewResult
import com.jimu.app.data.backup.BackupValidationResult
import com.jimu.app.data.backup.BackupValidator
import com.jimu.app.data.backup.ImportedCounts
import com.jimu.app.data.backup.ReminderRebuildResult
import com.jimu.app.data.backup.RestoreDatabaseResult
import com.jimu.app.data.backup.sampleBackupPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = ReviewMainDispatcherRule(dispatcher)

    @Test
    fun exportPreparationMovesToWaitingForLocation() = runTest(dispatcher) {
        val viewModel = createViewModel(exportJson = { """{"backupVersion":1}""" })

        viewModel.requestExport()
        assertEquals(SettingsUiState.Exporting, viewModel.uiState.value)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.WaitingForExportLocation)
        assertEquals("""{"backupVersion":1}""", (state as SettingsUiState.WaitingForExportLocation).json)
    }

    @Test
    fun cancelledExportLocationReturnsToIdleWithoutMessage() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.requestExport()
        advanceUntilIdle()

        viewModel.onExportLocationCancelled()

        assertEquals(SettingsUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun successfulExportWriteShowsSuccess() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.requestExport()
        advanceUntilIdle()
        viewModel.onExportLocationSelected()

        viewModel.onExportWritten(BackupFileWriteResult.Success)

        assertEquals(SettingsUiState.Success("备份已导出"), viewModel.uiState.value)
    }

    @Test
    fun failedExportWriteShowsError() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.requestExport()
        advanceUntilIdle()
        viewModel.onExportLocationSelected()

        viewModel.onExportWritten(BackupFileWriteResult.WriteFailed("disk full"))

        assertEquals(SettingsUiState.Error("disk full"), viewModel.uiState.value)
    }

    @Test
    fun validImportContentMovesToPreview() = runTest(dispatcher) {
        val ready = readyPreview()
        val viewModel = createViewModel(decode = { ready })

        viewModel.onImportPickerOpened()
        viewModel.onImportFileRead(BackupFileReadResult.Success("json"))
        advanceUntilIdle()

        assertEquals(SettingsUiState.PreviewReady(ready.preview, ready.validated), viewModel.uiState.value)
    }

    @Test
    fun oversizedImportFileShowsErrorWithoutDecoding() = runTest(dispatcher) {
        var decodeCalled = false
        val viewModel = createViewModel(
            decode = {
                decodeCalled = true
                readyPreview()
            }
        )

        viewModel.onImportFileRead(BackupFileReadResult.FileTooLarge())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SettingsUiState.Error)
        assertEquals(false, decodeCalled)
    }

    @Test
    fun invalidUtf8ImportShowsErrorWithoutDecoding() = runTest(dispatcher) {
        var decodeCalled = false
        val viewModel = createViewModel(
            decode = {
                decodeCalled = true
                readyPreview()
            }
        )

        viewModel.onImportFileRead(BackupFileReadResult.InvalidEncoding)
        advanceUntilIdle()

        assertEquals(
            SettingsUiState.Error("备份文件不是有效的 UTF-8 文本"),
            viewModel.uiState.value
        )
        assertEquals(false, decodeCalled)
    }

    @Test
    fun cancelledImportPickerReturnsToIdleWithoutMessage() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onImportPickerOpened()

        viewModel.onImportPickerCancelled()

        assertEquals(SettingsUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun cancellingPreImportBackupLocationAbortsRestore() = runTest(dispatcher) {
        var restoreCalled = false
        val ready = readyPreview()
        val viewModel = createViewModel(
            decode = { ready },
            restore = {
                restoreCalled = true
                restoreSuccess()
            }
        )
        viewModel.onImportFileRead(BackupFileReadResult.Success("json"))
        advanceUntilIdle()
        viewModel.confirmRestore()
        advanceUntilIdle()

        viewModel.onPreImportBackupLocationCancelled()

        assertEquals(SettingsUiState.Idle, viewModel.uiState.value)
        assertEquals(false, restoreCalled)
    }

    @Test
    fun preImportBackupWriteFailureDoesNotRestoreDatabase() = runTest(dispatcher) {
        var restoreCalled = false
        val ready = readyPreview()
        val viewModel = createViewModel(
            decode = { ready },
            restore = {
                restoreCalled = true
                restoreSuccess()
            }
        )
        viewModel.onImportFileRead(BackupFileReadResult.Success("json"))
        advanceUntilIdle()
        viewModel.confirmRestore()
        advanceUntilIdle()
        viewModel.onPreImportBackupLocationSelected()

        viewModel.onPreImportBackupWritten(
            BackupFileWriteResult.WriteFailed("disk full")
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SettingsUiState.Error)
        assertEquals(false, restoreCalled)
    }

    @Test
    fun reminderFailureAfterRestoreProducesPartialSuccess() = runTest(dispatcher) {
        val ready = readyPreview()
        val viewModel = createViewModel(
            decode = { ready },
            restore = { restoreSuccess() },
            rebuild = {
                ReminderRebuildResult.PartialFailure(
                    failedCancelIds = listOf(1L),
                    failedScheduleIds = listOf(2L)
                )
            }
        )
        viewModel.onImportFileRead(BackupFileReadResult.Success("json"))
        advanceUntilIdle()
        viewModel.confirmRestore()
        advanceUntilIdle()
        viewModel.onPreImportBackupLocationSelected()
        viewModel.onPreImportBackupWritten(BackupFileWriteResult.Success)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SettingsUiState.PartialSuccess)
    }

    private fun createViewModel(
        exportJson: suspend () -> String = { "{}" },
        decode: (String) -> BackupImportPreviewResult = { readyPreview() },
        restore: suspend (com.jimu.app.data.backup.ValidatedBackupPayload) -> RestoreDatabaseResult = {
            restoreSuccess()
        },
        rebuild: suspend (List<Long>) -> ReminderRebuildResult = {
            ReminderRebuildResult.Success
        }
    ): SettingsViewModel {
        return SettingsViewModel(
            createExportJson = exportJson,
            decodeAndValidate = decode,
            restoreValidatedPayload = restore,
            rebuildReminders = rebuild,
            ioDispatcher = dispatcher,
            timestampProvider = { "20260618-120000" }
        )
    }

    private fun readyPreview(): BackupImportPreviewResult.ReadyToConfirm {
        val payload = sampleBackupPayload()
        val validated = (BackupValidator.validate(payload) as BackupValidationResult.Valid).validated
        return BackupImportPreviewResult.ReadyToConfirm(
            validated = validated,
            preview = BackupImportPreview(
                exportedAt = payload.meta.exportedAt,
                tasks = 1,
                habits = 1,
                habitRecords = 1,
                goals = 1,
                goalSteps = 1,
                reviews = 1
            )
        )
    }

    private fun restoreSuccess(): RestoreDatabaseResult.Success {
        return RestoreDatabaseResult.Success(
            importedCounts = ImportedCounts(
                tasks = 1,
                habits = 1,
                habitRecords = 1,
                goals = 1,
                goalSteps = 1,
                reviews = 1
            ),
            oldTaskIds = listOf(1L)
        )
    }
}
