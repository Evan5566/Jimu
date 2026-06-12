package com.jimu.app.data.repository

import com.jimu.app.data.local.dao.GoalDao
import com.jimu.app.data.local.entity.GoalEntity
import com.jimu.app.data.local.entity.GoalStepEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class GoalUiModel(
    val goal: GoalEntity,
    val steps: List<GoalStepEntity>,
    val progress: Int
) {
    val totalStepCount: Int
        get() = steps.size

    val completedStepCount: Int
        get() = steps.count { it.isCompleted }

    val isCompleted: Boolean
        get() = totalStepCount > 0 && completedStepCount == totalStepCount
}

data class GoalOverviewUiModel(
    val totalGoals: Int,
    val completedGoals: Int,
    val totalSteps: Int,
    val completedSteps: Int,
    val progress: Int
)

class GoalRepository(
    private val goalDao: GoalDao
) {
    fun observeGoalUiModels(): Flow<List<GoalUiModel>> {
        return combine(
            goalDao.observeAllGoals(),
            goalDao.observeAllGoalSteps()
        ) { goals, steps ->
            goals.map { goal ->
                val goalSteps = steps
                    .filter { it.goalId == goal.id }
                    .sortedWith(
                        compareBy<GoalStepEntity> { it.isCompleted }
                            .thenBy { it.createdAt }
                    )

                GoalUiModel(
                    goal = goal,
                    steps = goalSteps,
                    progress = calculateProgress(goalSteps)
                )
            }
        }
    }

    fun observeGoalOverview(): Flow<GoalOverviewUiModel> {
        return observeGoalUiModels().map { goals ->
            val allSteps = goals.flatMap { it.steps }
            val completedSteps = allSteps.count { it.isCompleted }
            val completedGoals = goals.count { it.isCompleted }

            GoalOverviewUiModel(
                totalGoals = goals.size,
                completedGoals = completedGoals,
                totalSteps = allSteps.size,
                completedSteps = completedSteps,
                progress = calculateProgress(allSteps)
            )
        }
    }

    fun observeOverallProgress(): Flow<Int> {
        return observeGoalOverview().map { it.progress }
    }

    suspend fun addGoal(title: String) {
        val finalTitle = title.trim()
        if (finalTitle.isBlank()) return

        goalDao.insertGoal(
            GoalEntity(title = finalTitle)
        )
    }

    suspend fun updateGoalTitle(goal: GoalEntity, title: String) {
        val finalTitle = title.trim()
        if (finalTitle.isBlank()) return

        goalDao.updateGoal(
            goal.copy(
                title = finalTitle,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addGoalStep(goalId: Long, title: String) {
        val finalTitle = title.trim()
        if (finalTitle.isBlank()) return

        goalDao.insertGoalStep(
            GoalStepEntity(
                goalId = goalId,
                title = finalTitle
            )
        )
    }

    suspend fun updateGoalStepTitle(step: GoalStepEntity, title: String) {
        val finalTitle = title.trim()
        if (finalTitle.isBlank()) return

        goalDao.updateGoalStep(
            step.copy(
                title = finalTitle,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleGoalStep(step: GoalStepEntity) {
        goalDao.updateGoalStep(
            step.copy(
                isCompleted = !step.isCompleted,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun restoreGoal(goalId: Long) {
        goalDao.resetGoalStepsCompletion(
            goalId = goalId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoalStepsByGoalId(goal.id)
        goalDao.deleteGoal(goal)
    }

    suspend fun deleteGoalStep(step: GoalStepEntity) {
        goalDao.deleteGoalStep(step)
    }

    private fun calculateProgress(steps: List<GoalStepEntity>): Int {
        if (steps.isEmpty()) return 0
        return ((steps.count { it.isCompleted }.toFloat() / steps.size) * 100).toInt()
    }
}