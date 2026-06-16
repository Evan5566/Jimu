package com.jimu.app.data.repository

import com.jimu.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyDigestUiModel(
    val title: String = "今日成果草稿",
    val reviewDate: String,
    val taskOverview: String = "",
    val habitOverview: String = "",
    val goalOverview: String = "",
    val reminderOverview: String = "",
    val summaryLines: List<String> = emptyList(),
    val note: String = "根据当前数据整理，供参考，不代表精准统计。"
) {
    val isEmpty: Boolean
        get() = summaryLines.size == 1 && summaryLines.first().startsWith("今天还没有")

    companion object {
        fun empty(reviewDate: String = LocalDate.now().toString()): DailyDigestUiModel {
            return DailyDigestUiModel(
                reviewDate = reviewDate,
                summaryLines = listOf("今天还没有可整理的成果，先完成一个待办或打卡一个习惯。")
            )
        }
    }
}

class DailyDigestRepository(
    private val tasks: Flow<List<TaskEntity>>,
    private val habits: Flow<List<HabitUiModel>>,
    private val goals: Flow<List<GoalUiModel>>,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    constructor(
        taskRepository: TaskRepository,
        habitRepository: HabitRepository,
        goalRepository: GoalRepository
    ) : this(
        tasks = taskRepository.observeAllTasks(),
        habits = habitRepository.observeHabitUiModels(),
        goals = goalRepository.observeGoalUiModels()
    )

    fun observeDailyDigest(): Flow<DailyDigestUiModel> {
        return combine(tasks, habits, goals) { taskItems, habitItems, goalItems ->
            DailyDigestBuilder.build(
                tasks = taskItems,
                habits = habitItems,
                goals = goalItems,
                today = todayProvider(),
                zoneId = zoneId
            )
        }
    }
}

object DailyDigestBuilder {
    fun build(
        tasks: List<TaskEntity>,
        habits: List<HabitUiModel>,
        goals: List<GoalUiModel>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyDigestUiModel {
        val taskOverview = buildTaskOverview(tasks)
        val habitOverview = buildHabitOverview(habits)
        val goalOverview = buildGoalOverview(goals)
        val reminderOverview = buildReminderOverview(tasks, today, zoneId)

        val usefulLines = listOf(
            taskOverview,
            habitOverview,
            goalOverview,
            reminderOverview
        ).filter { it.isNotBlank() }

        if (usefulLines.isEmpty()) {
            return DailyDigestUiModel.empty(reviewDate = today.toString())
        }

        return DailyDigestUiModel(
            reviewDate = today.toString(),
            taskOverview = taskOverview,
            habitOverview = habitOverview,
            goalOverview = goalOverview,
            reminderOverview = reminderOverview,
            summaryLines = usefulLines.take(5)
        )
    }

    private fun buildTaskOverview(tasks: List<TaskEntity>): String {
        val completedTasks = tasks.filter { it.isCompleted }
        if (completedTasks.isEmpty()) return ""

        return "当前已完成 ${completedTasks.size} 项待办${completedTasks.namesSuffix()}。"
    }

    private fun buildHabitOverview(habits: List<HabitUiModel>): String {
        if (habits.isEmpty()) return ""

        val checkedHabits = habits.filter { it.checkedToday }
        if (checkedHabits.isEmpty()) return ""

        return "今日已打卡 ${checkedHabits.size}/${habits.size} 个习惯${checkedHabits.namesSuffix { it.habit.name }}。"
    }

    private fun buildGoalOverview(goals: List<GoalUiModel>): String {
        val totalSteps = goals.sumOf { it.totalStepCount }
        if (totalSteps == 0) return ""

        val completedSteps = goals.sumOf { it.completedStepCount }
        if (completedSteps == 0) return ""

        val progress = ((completedSteps.toFloat() / totalSteps) * 100).toInt()
        return "当前目标推进 $completedSteps/$totalSteps 个步骤，整体进度约 $progress%。"
    }

    private fun buildReminderOverview(
        tasks: List<TaskEntity>,
        today: LocalDate,
        zoneId: ZoneId
    ): String {
        val incompleteWithDates = tasks
            .asSequence()
            .filterNot { it.isCompleted }
            .mapNotNull { task ->
                task.dueDate?.let { dueDate ->
                    task to Instant.ofEpochMilli(dueDate).atZone(zoneId).toLocalDate()
                }
            }
            .toList()

        val todayCount = incompleteWithDates.count { (_, dueDate) -> dueDate == today }
        val overdueCount = incompleteWithDates.count { (_, dueDate) -> dueDate.isBefore(today) }

        val parts = buildList {
            if (todayCount > 0) add("$todayCount 项今日待处理")
            if (overdueCount > 0) add("$overdueCount 项逾期未完成")
        }

        if (parts.isEmpty()) return ""

        return "还有 ${parts.joinToString("、")}。"
    }

    private fun List<TaskEntity>.namesSuffix(): String {
        return namesSuffix { it.title }
    }

    private fun <T> List<T>.namesSuffix(nameOf: (T) -> String): String {
        val names = take(2).map(nameOf).filter { it.isNotBlank() }
        if (names.isEmpty()) return ""

        val suffix = if (size > 2) "等" else ""
        return "：${names.joinToString("、")}$suffix"
    }
}
