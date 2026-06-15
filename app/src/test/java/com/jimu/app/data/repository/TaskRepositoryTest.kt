package com.jimu.app.data.repository

import com.jimu.app.data.local.dao.TaskDao
import com.jimu.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TaskRepositoryTest {

    @Test
    fun addTaskReturnsInsertedTaskWithGeneratedId() = runBlocking {
        val dao = FakeTaskDao()
        val repository = TaskRepository(dao)

        val saved = repository.addTask(
            title = "  1分钟后提醒我测试待办  ",
            dueDate = 1_800_000_000_000L
        )

        assertNotNull(saved)
        assertEquals(1L, saved!!.id)
        assertEquals("1分钟后提醒我测试待办", saved.title)
        assertEquals(1_800_000_000_000L, saved.dueDate)
        assertEquals(false, saved.isCompleted)
        assertEquals(1, dao.snapshot().size)
    }

    @Test
    fun addTaskReturnsNullAndDoesNotInsertWhenTitleIsBlank() = runBlocking {
        val dao = FakeTaskDao()
        val repository = TaskRepository(dao)

        val saved = repository.addTask(title = "   ", dueDate = 1_800_000_000_000L)

        assertNull(saved)
        assertEquals(emptyList<TaskEntity>(), dao.snapshot())
    }

    @Test
    fun getFutureReminderTasksReturnsOnlyIncompleteTasksWithFutureDueDates() = runBlocking {
        val now = 1_800_000_000_000L
        val dao = FakeTaskDao()
        val repository = TaskRepository(dao)

        dao.seed(
            listOf(
                TaskEntity(id = 1L, title = "future", dueDate = now + 60_000L),
                TaskEntity(id = 2L, title = "past", dueDate = now - 1L),
                TaskEntity(id = 3L, title = "completed", dueDate = now + 60_000L, isCompleted = true),
                TaskEntity(id = 4L, title = "no due date", dueDate = null)
            )
        )

        val tasks = repository.getFutureReminderTasks(now)

        assertEquals(listOf(1L), tasks.map { it.id })
    }
}

private class FakeTaskDao : TaskDao {
    private val tasks = mutableListOf<TaskEntity>()
    private val tasksFlow = MutableStateFlow<List<TaskEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAllTasks(): Flow<List<TaskEntity>> = tasksFlow

    override fun observeCompletedTasks(): Flow<List<TaskEntity>> = tasksFlow

    override suspend fun insertTask(task: TaskEntity): Long {
        val id = if (task.id == 0L) nextId++ else task.id
        tasks.add(task.copy(id = id))
        publish()
        return id
    }

    override suspend fun updateTask(task: TaskEntity) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) {
            tasks[index] = task
        } else {
            tasks.add(task)
        }
        publish()
    }

    override suspend fun deleteTask(task: TaskEntity) {
        tasks.removeAll { it.id == task.id }
        publish()
    }

    override suspend fun getFutureReminderTasks(nowMillis: Long): List<TaskEntity> {
        return tasks
            .filter { task ->
                val dueDate = task.dueDate
                !task.isCompleted && dueDate != null && dueDate > nowMillis
            }
            .sortedBy { it.dueDate }
    }

    fun seed(seedTasks: List<TaskEntity>) {
        tasks.clear()
        tasks.addAll(seedTasks)
        nextId = (tasks.maxOfOrNull { it.id } ?: 0L) + 1L
        publish()
    }

    fun snapshot(): List<TaskEntity> = tasks.toList()

    private fun publish() {
        tasksFlow.value = snapshot()
    }
}
