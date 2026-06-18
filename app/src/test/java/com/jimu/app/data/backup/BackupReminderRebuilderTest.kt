package com.jimu.app.data.backup

import com.jimu.app.data.local.dao.TaskDao
import com.jimu.app.data.local.entity.TaskEntity
import com.jimu.app.data.repository.TaskRepository
import com.jimu.app.reminder.TaskReminderController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupReminderRebuilderTest {

    @Test
    fun reminderControllerContractDefaultsExactAlarmPermissionRequestToFalse() {
        val calls = mutableListOf<String>()
        val controller: TaskReminderController = RecordingReminderController(calls)

        controller.schedule(TaskEntity(id = 9L, title = "default permission"))

        assertEquals(listOf("schedule:9:false"), calls)
    }

    @Test
    fun cancelsAllOldIdsBeforeSchedulingRestoredFutureTasks() = runBlocking {
        val calls = mutableListOf<String>()
        val controller = RecordingReminderController(calls)
        val repository = TaskRepository(
            ReminderTaskDao(
                listOf(
                    TaskEntity(id = 2L, title = "restored 2", dueDate = 2_000L),
                    TaskEntity(id = 4L, title = "restored 4", dueDate = 3_000L)
                )
            )
        )
        val rebuilder = BackupReminderRebuilder(
            taskRepository = repository,
            reminderController = controller,
            nowMillis = { 1_000L }
        )

        val result = rebuilder.rebuildAfterSuccessfulRestore(listOf(1L, 2L, 3L))

        assertEquals(
            listOf(
                "cancel:1",
                "cancel:2",
                "cancel:3",
                "schedule:2:false",
                "schedule:4:false"
            ),
            calls
        )
        assertEquals(ReminderRebuildResult.Success, result)
    }

    @Test
    fun partialFailuresDoNotStopRemainingReminderOperations() = runBlocking {
        val calls = mutableListOf<String>()
        val controller = RecordingReminderController(
            calls = calls,
            failCancelIds = setOf(2L),
            failScheduleIds = setOf(4L)
        )
        val repository = TaskRepository(
            ReminderTaskDao(
                listOf(
                    TaskEntity(id = 2L, title = "restored 2", dueDate = 2_000L),
                    TaskEntity(id = 4L, title = "restored 4", dueDate = 3_000L)
                )
            )
        )
        val rebuilder = BackupReminderRebuilder(
            taskRepository = repository,
            reminderController = controller,
            nowMillis = { 1_000L }
        )

        val result = rebuilder.rebuildAfterSuccessfulRestore(listOf(1L, 2L, 3L))

        assertEquals(
            listOf(
                "cancel:1",
                "cancel:2",
                "cancel:3",
                "schedule:2:false",
                "schedule:4:false"
            ),
            calls
        )
        assertEquals(
            ReminderRebuildResult.PartialFailure(
                failedCancelIds = listOf(2L),
                failedScheduleIds = listOf(4L)
            ),
            result
        )
    }

    @Test
    fun restoredTaskQueryFailureIsReportedAfterOldCancelsAreAttempted() = runBlocking {
        val calls = mutableListOf<String>()
        val rebuilder = BackupReminderRebuilder(
            taskRepository = TaskRepository(ReminderTaskDao(errorOnQuery = true)),
            reminderController = RecordingReminderController(calls),
            nowMillis = { 1_000L }
        )

        val result = rebuilder.rebuildAfterSuccessfulRestore(listOf(1L, 2L))

        assertEquals(listOf("cancel:1", "cancel:2"), calls)
        assertTrue(result is ReminderRebuildResult.Failed)
    }
}

private class RecordingReminderController(
    private val calls: MutableList<String>,
    private val failCancelIds: Set<Long> = emptySet(),
    private val failScheduleIds: Set<Long> = emptySet()
) : TaskReminderController {
    override fun cancel(taskId: Long) {
        calls += "cancel:$taskId"
        if (taskId in failCancelIds) error("cancel failed")
    }

    override fun schedule(
        task: TaskEntity,
        mayRequestExactAlarmPermission: Boolean
    ) {
        calls += "schedule:${task.id}:$mayRequestExactAlarmPermission"
        if (task.id in failScheduleIds) error("schedule failed")
    }
}

private class ReminderTaskDao(
    private val futureTasks: List<TaskEntity> = emptyList(),
    private val errorOnQuery: Boolean = false
) : TaskDao {
    private val flow = MutableStateFlow(futureTasks)

    override fun observeAllTasks(): Flow<List<TaskEntity>> = flow
    override fun observeCompletedTasks(): Flow<List<TaskEntity>> = flow
    override suspend fun getFutureReminderTasks(nowMillis: Long): List<TaskEntity> {
        if (errorOnQuery) error("query failed")
        return futureTasks
    }
    override suspend fun getAllTasksForBackup(): List<TaskEntity> = futureTasks
    override suspend fun insertTask(task: TaskEntity): Long = task.id
    override suspend fun insertTasksForRestoreAbort(tasks: List<TaskEntity>) = Unit
    override suspend fun updateTask(task: TaskEntity) = Unit
    override suspend fun deleteTask(task: TaskEntity) = Unit
    override suspend fun deleteAllTasksForRestore() = Unit
}
