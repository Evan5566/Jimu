package com.jimu.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reviewDate: String,
    val type: String = "daily",
    val summary: String,
    val problems: String,
    val tomorrowFocus: String,
    val mood: Int? = null,
    val completedTaskSnapshot: Int = 0,
    val checkedHabitSnapshot: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
