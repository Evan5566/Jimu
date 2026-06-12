package com.jimu.app

import android.app.Application
import androidx.room.Room
import com.jimu.app.data.local.AppDatabase
import com.jimu.app.data.local.MIGRATION_4_5
import com.jimu.app.data.repository.GoalRepository
import com.jimu.app.data.repository.HabitRepository
import com.jimu.app.data.repository.ReviewRepository
import com.jimu.app.data.repository.TaskRepository

class JimuApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var taskRepository: TaskRepository
        private set

    lateinit var habitRepository: HabitRepository
        private set

    lateinit var goalRepository: GoalRepository
        private set

    lateinit var reviewRepository: ReviewRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "jimu_database"
        )
            .addMigrations(MIGRATION_4_5)
            .build()

        taskRepository = TaskRepository(database.taskDao())
        habitRepository = HabitRepository(database.habitDao())
        goalRepository = GoalRepository(database.goalDao())
        reviewRepository = ReviewRepository(database.reviewDao())
    }
}
