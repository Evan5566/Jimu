package com.jimu.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)