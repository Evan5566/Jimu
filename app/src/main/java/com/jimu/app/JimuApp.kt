package com.jimu.app

import android.app.Application
import androidx.room.Room
import com.jimu.app.data.local.AppDatabase
import com.jimu.app.data.local.MIGRATION_4_5
import com.jimu.app.data.repository.DailyDigestRepository
import com.jimu.app.data.repository.GoalRepository
import com.jimu.app.data.repository.HabitRepository
import com.jimu.app.data.repository.ReviewRepository
import com.jimu.app.data.repository.TaskRepository
import com.jimu.app.reminder.TaskReminderNotifier
import com.jimu.app.reminder.TaskReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JimuApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    lateinit var dailyDigestRepository: DailyDigestRepository
        private set

    lateinit var taskReminderScheduler: TaskReminderScheduler
        private set

    override fun onCreate() {
        super.onCreate()

        TaskReminderNotifier.createNotificationChannel(this)
        taskReminderScheduler = TaskReminderScheduler(this)

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
        dailyDigestRepository = DailyDigestRepository(
            taskRepository = taskRepository,
            habitRepository = habitRepository,
            goalRepository = goalRepository
        )

        restoreFutureTaskRemindersAsync()
    }

    fun restoreFutureTaskRemindersAsync() {
        applicationScope.launch {
            restoreFutureTaskReminders()
        }
    }

    suspend fun restoreFutureTaskReminders() {
        withContext(Dispatchers.IO) {
            val tasks = taskRepository.getFutureReminderTasks(System.currentTimeMillis())
            tasks.forEach(taskReminderScheduler::schedule)
        }
    }
}
