package com.jimu.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jimu.app.data.local.entity.GoalEntity
import com.jimu.app.data.local.entity.GoalStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun observeAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goal_steps ORDER BY createdAt ASC")
    fun observeAllGoalSteps(): Flow<List<GoalStepEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalStep(step: GoalStepEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoalStep(step: GoalStepEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoalStep(step: GoalStepEntity)

    @Query("DELETE FROM goal_steps WHERE goalId = :goalId")
    suspend fun deleteGoalStepsByGoalId(goalId: Long)

    @Query(
        """
        UPDATE goal_steps
        SET isCompleted = 0, updatedAt = :updatedAt
        WHERE goalId = :goalId
        """
    )
    suspend fun resetGoalStepsCompletion(goalId: Long, updatedAt: Long)
}