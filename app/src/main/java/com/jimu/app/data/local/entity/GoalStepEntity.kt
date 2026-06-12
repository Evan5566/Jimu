package com.jimu.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal_steps")
data class GoalStepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)