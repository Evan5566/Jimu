package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jimu.app.data.repository.GoalRepository
import com.jimu.app.data.repository.GoalUiModel
import com.jimu.app.data.repository.ReviewRepository
import com.jimu.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeGoalFocusUiModel(
    val periodLabel: String = "当前",
    val goalCount: Int = 0,
    val completedGoalCount: Int = 0,
    val totalSteps: Int = 0,
    val completedSteps: Int = 0,
    val progress: Int = 0
) {
    val hasGoals: Boolean
        get() = goalCount > 0
}

data class HomeTodayReviewUiModel(
    val reviewDate: String = LocalDate.now().toString(),
    val hasReview: Boolean = false,
    val summary: String = ""
) {
    val displaySummary: String
        get() = summary.ifBlank { "今日复盘已保存。" }
}

class HomeViewModel(
    taskRepository: TaskRepository,
    goalRepository: GoalRepository,
    reviewRepository: ReviewRepository
) : ViewModel() {

    val todoCount = taskRepository.observeAllTasks()
        .map { tasks ->
            val today = LocalDate.now()
            val zoneId = ZoneId.systemDefault()

            tasks.count { task ->
                if (task.isCompleted) return@count false

                val dueDate = task.dueDate
                if (dueDate == null) return@count false

                val dueLocalDate = Instant.ofEpochMilli(dueDate)
                    .atZone(zoneId)
                    .toLocalDate()

                dueLocalDate.isEqual(today) || dueLocalDate.isBefore(today)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val completedCount = taskRepository.observeCompletedTasks()
        .map { tasks -> tasks.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val goalFocus = goalRepository.observeGoalUiModels()
        .map { goals ->
            buildGoalFocus(goals)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeGoalFocusUiModel()
        )

    val todayReview = reviewRepository.observeAllReviews()
        .map { reviews ->
            val today = LocalDate.now().toString()
            val review = reviews
                .filter { review ->
                    review.reviewDate == today && review.type == "daily"
                }
                .maxByOrNull { review -> review.updatedAt }

            HomeTodayReviewUiModel(
                reviewDate = today,
                hasReview = review != null,
                summary = review?.summary?.trim().orEmpty()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeTodayReviewUiModel()
        )

    private fun buildGoalFocus(goals: List<GoalUiModel>): HomeGoalFocusUiModel {
        if (goals.isEmpty()) {
            return HomeGoalFocusUiModel()
        }

        val weekGoals = goals.filter { detectGoalPeriod(it.goal.title) == GoalPeriodType.WEEK }
        val monthGoals = goals.filter { detectGoalPeriod(it.goal.title) == GoalPeriodType.MONTH }
        val yearGoals = goals.filter { detectGoalPeriod(it.goal.title) == GoalPeriodType.YEAR }
        val otherGoals = goals.filter { detectGoalPeriod(it.goal.title) == GoalPeriodType.OTHER }

        val focusedGoals = when {
            weekGoals.isNotEmpty() -> weekGoals
            monthGoals.isNotEmpty() -> monthGoals
            yearGoals.isNotEmpty() -> yearGoals
            else -> otherGoals
        }

        val periodLabel = when {
            weekGoals.isNotEmpty() -> "本周"
            monthGoals.isNotEmpty() -> "本月"
            yearGoals.isNotEmpty() -> "本年"
            else -> "其他"
        }

        val allSteps = focusedGoals.flatMap { it.steps }
        val completedSteps = allSteps.count { it.isCompleted }
        val progress = if (allSteps.isEmpty()) {
            0
        } else {
            ((completedSteps.toFloat() / allSteps.size) * 100).toInt()
        }

        return HomeGoalFocusUiModel(
            periodLabel = periodLabel,
            goalCount = focusedGoals.size,
            completedGoalCount = focusedGoals.count { it.isCompleted },
            totalSteps = allSteps.size,
            completedSteps = completedSteps,
            progress = progress
        )
    }

    private fun detectGoalPeriod(title: String): GoalPeriodType {
        val normalized = title.trim()

        return when {
            normalized.startsWith("本周") ||
                    normalized.startsWith("这周") ||
                    normalized.startsWith("这个星期") ||
                    normalized.startsWith("这星期") ||
                    normalized.startsWith("这一周") ||
                    normalized.startsWith("当周") -> GoalPeriodType.WEEK

            normalized.startsWith("本月") ||
                    normalized.startsWith("这个月") ||
                    normalized.startsWith("这月") ||
                    normalized.startsWith("当月") ||
                    normalized.startsWith("这一个月") -> GoalPeriodType.MONTH

            normalized.startsWith("本年") ||
                    normalized.startsWith("今年") ||
                    normalized.startsWith("这一年") ||
                    normalized.startsWith("当年") -> GoalPeriodType.YEAR

            else -> GoalPeriodType.OTHER
        }
    }
}

private enum class GoalPeriodType {
    WEEK,
    MONTH,
    YEAR,
    OTHER
}

class HomeViewModelFactory(
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val reviewRepository: ReviewRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(taskRepository, goalRepository, reviewRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
