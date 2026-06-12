package com.jimu.app.data.repository

import com.jimu.app.data.local.dao.TaskDao
import com.jimu.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao
) {
    fun observeAllTasks(): Flow<List<TaskEntity>> = taskDao.observeAllTasks()

    fun observeCompletedTasks(): Flow<List<TaskEntity>> = taskDao.observeCompletedTasks()

    suspend fun addTask(
        title: String,
        dueDate: Long? = null
    ) {
        val finalTitle = title.trim()
        if (finalTitle.isBlank()) return

        taskDao.insertTask(
            TaskEntity(
                title = finalTitle,
                dueDate = dueDate
            )
        )
    }

    suspend fun toggleTaskCompleted(task: TaskEntity) {
        taskDao.updateTask(
            task.copy(
                isCompleted = !task.isCompleted,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    suspend fun rescheduleTask(task: TaskEntity, dueDate: Long?) {
        taskDao.updateTask(
            task.copy(
                dueDate = dueDate,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateTask(
        task: TaskEntity,
        title: String,
        description: String,
        dueDate: Long? = task.dueDate
    ) {
        val finalTitle = title.trim()
        if (finalTitle.isBlank()) return

        taskDao.updateTask(
            task.copy(
                title = finalTitle,
                description = description.trim().ifBlank { null },
                dueDate = dueDate,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}