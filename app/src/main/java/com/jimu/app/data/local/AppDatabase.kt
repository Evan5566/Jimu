package com.jimu.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitRecordEntity::class,
        GoalEntity::class,
        GoalStepEntity::class,
        ReviewEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun reviewDao(): ReviewDao
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `daily_reviews` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `reviewDate` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `summary` TEXT NOT NULL,
                `problems` TEXT NOT NULL,
                `tomorrowFocus` TEXT NOT NULL,
                `mood` INTEGER,
                `completedTaskSnapshot` INTEGER NOT NULL,
                `checkedHabitSnapshot` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
