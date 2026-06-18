package com.jimu.app.data.backup

import com.jimu.app.data.local.dao.GoalDao
import com.jimu.app.data.local.dao.HabitDao
import com.jimu.app.data.local.dao.ReviewDao
import com.jimu.app.data.local.dao.TaskDao
import com.jimu.app.data.local.entity.GoalEntity
import com.jimu.app.data.local.entity.GoalStepEntity
import com.jimu.app.data.local.entity.HabitEntity
import com.jimu.app.data.local.entity.HabitRecordEntity
import com.jimu.app.data.local.entity.ReviewEntity
import com.jimu.app.data.local.entity.TaskEntity

class BackupRepository(
    private val transactionRunner: BackupTransactionRunner,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val goalDao: GoalDao,
    private val reviewDao: ReviewDao
) {

    suspend fun createCurrentBackup(meta: BackupMetaV1): AppBackupPayloadV1 {
        return transactionRunner.runInTransaction {
            AppBackupPayloadV1(
                meta = meta,
                tasks = taskDao.getAllTasksForBackup().map(TaskEntity::toBackupV1),
                habits = habitDao.getAllHabitsForBackup().map(HabitEntity::toBackupV1),
                habitRecords = habitDao.getAllHabitRecordsForBackup()
                    .map(HabitRecordEntity::toBackupV1),
                goals = goalDao.getAllGoalsForBackup().map(GoalEntity::toBackupV1),
                goalSteps = goalDao.getAllGoalStepsForBackup().map(GoalStepEntity::toBackupV1),
                reviews = reviewDao.getAllReviewsForBackup().map(ReviewEntity::toBackupV1)
            )
        }
    }

    suspend fun exportCurrentBackupJson(meta: BackupMetaV1): String {
        val json = BackupJsonCodec.encode(createCurrentBackup(meta))
        if (json.toByteArray(Charsets.UTF_8).size > MAX_BACKUP_BYTES) {
            throw BackupTooLargeException()
        }
        return json
    }

    fun decodeAndValidate(json: String): BackupImportPreviewResult {
        return when (val decoded = BackupJsonCodec.decode(json)) {
            is BackupDecodeResult.InvalidFormat -> {
                BackupImportPreviewResult.InvalidFormat(decoded.message)
            }
            is BackupDecodeResult.UnsupportedVersion -> {
                BackupImportPreviewResult.UnsupportedVersion(decoded.version)
            }
            is BackupDecodeResult.Success -> {
                when (val validation = BackupValidator.validate(decoded.payload)) {
                    is BackupValidationResult.Invalid -> {
                        BackupImportPreviewResult.ValidationFailed(validation.message)
                    }
                    is BackupValidationResult.Valid -> {
                        val payload = validation.validated.payload
                        BackupImportPreviewResult.ReadyToConfirm(
                            validated = validation.validated,
                            preview = BackupImportPreview(
                                exportedAt = payload.meta.exportedAt,
                                tasks = payload.tasks.size,
                                habits = payload.habits.size,
                                habitRecords = payload.habitRecords.size,
                                goals = payload.goals.size,
                                goalSteps = payload.goalSteps.size,
                                reviews = payload.reviews.size
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun restoreValidatedPayload(
        validated: ValidatedBackupPayload
    ): RestoreDatabaseResult {
        return try {
            transactionRunner.runInTransaction {
                val oldTaskIds = taskDao.getAllTasksForBackup().map(TaskEntity::id)
                val payload = validated.payload

                habitDao.deleteAllHabitRecordsForRestore()
                goalDao.deleteAllGoalStepsForRestore()
                reviewDao.deleteAllReviewsForRestore()
                taskDao.deleteAllTasksForRestore()
                habitDao.deleteAllHabitsForRestore()
                goalDao.deleteAllGoalsForRestore()

                payload.tasks.map(TaskBackupV1::toEntity)
                    .takeIf(List<TaskEntity>::isNotEmpty)
                    ?.let { taskDao.insertTasksForRestoreAbort(it) }
                payload.habits.map(HabitBackupV1::toEntity)
                    .takeIf(List<HabitEntity>::isNotEmpty)
                    ?.let { habitDao.insertHabitsForRestoreAbort(it) }
                payload.goals.map(GoalBackupV1::toEntity)
                    .takeIf(List<GoalEntity>::isNotEmpty)
                    ?.let { goalDao.insertGoalsForRestoreAbort(it) }
                payload.reviews.map(ReviewBackupV1::toEntity)
                    .takeIf(List<ReviewEntity>::isNotEmpty)
                    ?.let { reviewDao.insertReviewsForRestoreAbort(it) }
                payload.habitRecords.map(HabitRecordBackupV1::toEntity)
                    .takeIf(List<HabitRecordEntity>::isNotEmpty)
                    ?.let { habitDao.insertHabitRecordsForRestoreAbort(it) }
                payload.goalSteps.map(GoalStepBackupV1::toEntity)
                    .takeIf(List<GoalStepEntity>::isNotEmpty)
                    ?.let { goalDao.insertGoalStepsForRestoreAbort(it) }

                RestoreDatabaseResult.Success(
                    importedCounts = ImportedCounts(
                        tasks = payload.tasks.size,
                        habits = payload.habits.size,
                        habitRecords = payload.habitRecords.size,
                        goals = payload.goals.size,
                        goalSteps = payload.goalSteps.size,
                        reviews = payload.reviews.size
                    ),
                    oldTaskIds = oldTaskIds
                )
            }
        } catch (error: Exception) {
            RestoreDatabaseResult.RestoreFailed(error.message ?: "恢复数据库失败")
        }
    }
}

private fun TaskEntity.toBackupV1() = TaskBackupV1(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun HabitEntity.toBackupV1() = HabitBackupV1(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt
)

private fun HabitRecordEntity.toBackupV1() = HabitRecordBackupV1(
    id = id,
    habitId = habitId,
    recordDate = recordDate,
    createdAt = createdAt
)

private fun GoalEntity.toBackupV1() = GoalBackupV1(
    id = id,
    title = title,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun GoalStepEntity.toBackupV1() = GoalStepBackupV1(
    id = id,
    goalId = goalId,
    title = title,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun ReviewEntity.toBackupV1() = ReviewBackupV1(
    id = id,
    reviewDate = reviewDate,
    type = type,
    summary = summary,
    problems = problems,
    tomorrowFocus = tomorrowFocus,
    mood = mood,
    completedTaskSnapshot = completedTaskSnapshot,
    checkedHabitSnapshot = checkedHabitSnapshot,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun TaskBackupV1.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    dueDate = dueDate,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun HabitBackupV1.toEntity() = HabitEntity(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt
)

private fun HabitRecordBackupV1.toEntity() = HabitRecordEntity(
    id = id,
    habitId = habitId,
    recordDate = recordDate,
    createdAt = createdAt
)

private fun GoalBackupV1.toEntity() = GoalEntity(
    id = id,
    title = title,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun GoalStepBackupV1.toEntity() = GoalStepEntity(
    id = id,
    goalId = goalId,
    title = title,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun ReviewBackupV1.toEntity() = ReviewEntity(
    id = id,
    reviewDate = reviewDate,
    type = type,
    summary = summary,
    problems = problems,
    tomorrowFocus = tomorrowFocus,
    mood = mood,
    completedTaskSnapshot = completedTaskSnapshot,
    checkedHabitSnapshot = checkedHabitSnapshot,
    createdAt = createdAt,
    updatedAt = updatedAt
)
