package com.jimu.app.ui.completed

import com.jimu.app.data.local.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class CompletedStats(
    val totalCount: Int,
    val latestText: String
)

internal fun buildCompletedStats(tasks: List<TaskEntity>): CompletedStats {
    val latestText = tasks.maxByOrNull { it.updatedAt }?.let {
        "最近一次记录：${formatCompletedRecordDateTime(it.updatedAt)}"
    } ?: "最近还没有完成记录"

    return CompletedStats(
        totalCount = tasks.size,
        latestText = latestText
    )
}

private fun formatCompletedRecordDateTime(timestamp: Long): String {
    return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(timestamp))
}
