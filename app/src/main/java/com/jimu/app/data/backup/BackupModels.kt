package com.jimu.app.data.backup

const val BACKUP_VERSION = 1
const val BACKUP_APP_PACKAGE = "com.jimu.app"
const val MAX_BACKUP_BYTES = 10 * 1024 * 1024

data class BackupMetaV1(
    val exportedAt: Long,
    val appPackage: String,
    val appVersionName: String,
    val appVersionCode: Long
)

data class AppBackupPayloadV1(
    val backupVersion: Int = BACKUP_VERSION,
    val meta: BackupMetaV1,
    val tasks: List<TaskBackupV1>,
    val habits: List<HabitBackupV1>,
    val habitRecords: List<HabitRecordBackupV1>,
    val goals: List<GoalBackupV1>,
    val goalSteps: List<GoalStepBackupV1>,
    val reviews: List<ReviewBackupV1>
)

data class TaskBackupV1(
    val id: Long,
    val title: String,
    val description: String?,
    val dueDate: Long?,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class HabitBackupV1(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Long
)

data class HabitRecordBackupV1(
    val id: Long,
    val habitId: Long,
    val recordDate: String,
    val createdAt: Long
)

data class GoalBackupV1(
    val id: Long,
    val title: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class GoalStepBackupV1(
    val id: Long,
    val goalId: Long,
    val title: String,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class ReviewBackupV1(
    val id: Long,
    val reviewDate: String,
    val type: String,
    val summary: String,
    val problems: String,
    val tomorrowFocus: String,
    val mood: Int?,
    val completedTaskSnapshot: Int,
    val checkedHabitSnapshot: Int,
    val createdAt: Long,
    val updatedAt: Long
)

sealed interface BackupDecodeResult {
    data class Success(val payload: AppBackupPayloadV1) : BackupDecodeResult
    data class InvalidFormat(val message: String) : BackupDecodeResult
    data class UnsupportedVersion(val version: Int) : BackupDecodeResult
}

class ValidatedBackupPayload internal constructor(
    val payload: AppBackupPayloadV1
)

sealed interface BackupValidationResult {
    data class Valid(val validated: ValidatedBackupPayload) : BackupValidationResult
    data class Invalid(val message: String) : BackupValidationResult
}

data class BackupImportPreview(
    val exportedAt: Long,
    val tasks: Int,
    val habits: Int,
    val habitRecords: Int,
    val goals: Int,
    val goalSteps: Int,
    val reviews: Int
)

sealed interface BackupImportPreviewResult {
    data class ReadyToConfirm(
        val validated: ValidatedBackupPayload,
        val preview: BackupImportPreview
    ) : BackupImportPreviewResult

    data class InvalidFormat(val message: String) : BackupImportPreviewResult
    data class UnsupportedVersion(val version: Int) : BackupImportPreviewResult
    data class ValidationFailed(val message: String) : BackupImportPreviewResult
}

data class ImportedCounts(
    val tasks: Int,
    val habits: Int,
    val habitRecords: Int,
    val goals: Int,
    val goalSteps: Int,
    val reviews: Int
)

sealed interface RestoreDatabaseResult {
    data class Success(
        val importedCounts: ImportedCounts,
        val oldTaskIds: List<Long>
    ) : RestoreDatabaseResult

    data class RestoreFailed(val message: String) : RestoreDatabaseResult
}

class BackupTooLargeException : IllegalStateException("备份内容超过 10 MiB 上限")

sealed interface BackupFileReadResult {
    data class Success(val content: String) : BackupFileReadResult
    data class FileTooLarge(val limitBytes: Int = MAX_BACKUP_BYTES) : BackupFileReadResult
    data object InvalidEncoding : BackupFileReadResult
    data class ReadFailed(val message: String) : BackupFileReadResult
}

sealed interface BackupFileWriteResult {
    data object Success : BackupFileWriteResult
    data class FileTooLarge(val limitBytes: Int = MAX_BACKUP_BYTES) : BackupFileWriteResult
    data class WriteFailed(val message: String) : BackupFileWriteResult
}
