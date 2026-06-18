package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jimu.app.data.backup.BackupMetaV1
import com.jimu.app.data.backup.BackupReminderRebuilder
import com.jimu.app.data.backup.BackupRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SettingsViewModelFactory(
    private val backupRepository: BackupRepository,
    private val reminderRebuilder: BackupReminderRebuilder,
    private val appPackage: String,
    private val appVersionName: String,
    private val appVersionCode: Long
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                createExportJson = {
                    backupRepository.exportCurrentBackupJson(
                        BackupMetaV1(
                            exportedAt = System.currentTimeMillis(),
                            appPackage = appPackage,
                            appVersionName = appVersionName,
                            appVersionCode = appVersionCode
                        )
                    )
                },
                decodeAndValidate = backupRepository::decodeAndValidate,
                restoreValidatedPayload = backupRepository::restoreValidatedPayload,
                rebuildReminders = reminderRebuilder::rebuildAfterSuccessfulRestore,
                timestampProvider = {
                    LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT)
                }
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

    private companion object {
        val FILE_TIMESTAMP_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    }
}
