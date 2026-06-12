package com.jimu.app

import android.app.Application
import androidx.room.Room
import com.jimu.app.data.local.AppDatabase
import com.jimu.app.data.repository.TaskRepository
import com.jimu.app.data.repository.HabitRepository
import com.jimu.app.data.repository.GoalRepository

class JimuApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var habitRepository: HabitRepository
        private set

    lateinit var goalRepository: GoalRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "jimu_database"
        )
            .fallbackToDestructiveMigration()
            .build()

        taskRepository = TaskRepository(database.taskDao())
        habitRepository = HabitRepository(database.habitDao())
        goalRepository = GoalRepository(database.goalDao())
    }
}