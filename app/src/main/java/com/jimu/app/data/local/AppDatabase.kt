package com.jimu.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jimu.app.data.local.dao.GoalDao
import com.jimu.app.data.local.dao.HabitDao
import com.jimu.app.data.local.dao.TaskDao
import com.jimu.app.data.local.entity.GoalEntity
import com.jimu.app.data.local.entity.GoalStepEntity
import com.jimu.app.data.local.entity.HabitEntity
import com.jimu.app.data.local.entity.HabitRecordEntity
import com.jimu.app.data.local.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitRecordEntity::class,
        GoalEntity::class,
        GoalStepEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
}
