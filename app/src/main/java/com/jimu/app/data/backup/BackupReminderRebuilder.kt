package com.jimu.app.data.backup

import com.jimu.app.data.repository.TaskRepository
import com.jimu.app.reminder.TaskReminderController

sealed interface ReminderRebuildResult {
    data object Success : ReminderRebuildResult

    data class PartialFailure(
        val failedCancelIds: List<Long>,
        val failedScheduleIds: List<Long>
    ) : ReminderRebuildResult

    data class Failed(
        val message: String,
        val failedCancelIds: List<Long>
    ) : ReminderRebuildResult
}

class BackupReminderRebuilder(
    private val taskRepository: TaskRepository,
    private val reminderController: TaskReminderController,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    suspend fun rebuildAfterSuccessfulRestore(
        oldTaskIds: List<Long>
    ): ReminderRebuildResult {
        val failedCancelIds = buildList {
            oldTaskIds.forEach { taskId ->
                runCatching { reminderController.cancel(taskId) }
                    .onFailure { add(taskId) }
            }
        }

        val restoredFutureTasks = runCatching {
            taskRepository.getFutureReminderTasks(nowMillis())
        }.getOrElse { error ->
            return ReminderRebuildResult.Failed(
                message = error.message ?: "读取恢复后的提醒任务失败",
                failedCancelIds = failedCancelIds
            )
        }

        val failedScheduleIds = buildList {
            restoredFutureTasks.forEach { task ->
                runCatching {
                    reminderController.schedule(
                        task = task,
                        mayRequestExactAlarmPermission = false
                    )
                }.onFailure { add(task.id) }
            }
        }

        return if (failedCancelIds.isEmpty() && failedScheduleIds.isEmpty()) {
            ReminderRebuildResult.Success
        } else {
            ReminderRebuildResult.PartialFailure(
                failedCancelIds = failedCancelIds,
                failedScheduleIds = failedScheduleIds
            )
        }
    }
}
