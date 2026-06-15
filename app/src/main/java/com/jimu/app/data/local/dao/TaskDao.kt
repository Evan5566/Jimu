package com.jimu.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jimu.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAllTasks(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun observeCompletedTasks(): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks " +
            "WHERE isCompleted = 0 AND dueDate IS NOT NULL AND dueDate > :nowMillis " +
            "ORDER BY dueDate ASC"
    )
    suspend fun getFutureReminderTasks(nowMillis: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}
