package com.jimu.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_records")
data class HabitRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val recordDate: String,
    val createdAt: Long = System.currentTimeMillis()
)