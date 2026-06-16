package com.jimu.app.ui.completed

import com.jimu.app.data.local.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class CompletedGroup(
    val dayStart: Long,
    val title: String,
    val tasks: List<TaskEntity>
)

internal fun buildCompletedGroups(tasks: List<TaskEntity>): List<CompletedGroup> {
    if (tasks.isEmpty()) return emptyList()

    val grouped = linkedMapOf<Long, MutableList<TaskEntity>>()

    tasks.sortedByDescending { it.updatedAt }.forEach { task ->
        val dayStart = startOfDay(task.updatedAt)
        grouped.getOrPut(dayStart) { mutableListOf() }.add(task)
    }

    return grouped.entries
        .sortedByDescending { it.key }
        .map { entry ->
            CompletedGroup(
                dayStart = entry.key,
                title = formatGroupTitle(entry.key),
                tasks = entry.value
            )
        }
}

private fun formatGroupTitle(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val todayStart = startOfDay(now)
    val yesterdayStart = todayStart - 24L * 60L * 60L * 1000L

    return when (timestamp) {
        todayStart -> "今天"
        yesterdayStart -> "昨天"
        else -> SimpleDateFormat("M月d日", Locale.CHINA).format(Date(timestamp))
    }
}

private fun startOfDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
