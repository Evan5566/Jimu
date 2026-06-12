package com.jimu.app.ui.completed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jimu.app.JimuApp
import com.jimu.app.data.local.entity.TaskEntity
import com.jimu.app.ui.theme.PanelBlue
import com.jimu.app.viewmodel.CompletedViewModel
import com.jimu.app.viewmodel.CompletedViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CompletedScreen(innerPadding: PaddingValues) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as JimuApp
    val viewModel: CompletedViewModel = viewModel(
        factory = CompletedViewModelFactory(app.taskRepository)
    )

    val completedTasks by viewModel.completedTasks.collectAsState()
    val stats = remember(completedTasks) { buildCompletedStats(completedTasks) }
    val groupedTasks = remember(completedTasks) { buildCompletedGroups(completedTasks) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = innerPadding.calculateBottomPadding() + 12.dp
            )
    ) {
        PageHeader(
            title = "已完成",
            subtitle = "回看已经推进过的事项，保持节奏感。"
        )

        Spacer(modifier = Modifier.height(16.dp))

        CompletedSummaryCard(stats = stats)

        Spacer(modifier = Modifier.height(18.dp))

        SectionHeader(
            title = "完成记录",
            subtitle = if (completedTasks.isEmpty()) null else "按完成时间归档，可点状态回退。"
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (completedTasks.isEmpty()) {
            EmptyStateCard()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                groupedTasks.forEach { group ->
                    item(key = "header_${group.dayStart}") {
                        GroupHeader(title = group.title)
                    }

                    items(
                        items = group.tasks,
                        key = { it.id }
                    ) { task ->
                        CompletedTaskCard(
                            task = task,
                            onRevert = { viewModel.toggleTaskCompleted(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompletedSummaryCard(
    stats: CompletedStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "完成节奏",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    value = stats.totalCount.toString(),
                    label = "累计完成"
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    value = stats.todayCount.toString(),
                    label = "今天完成"
                )
                SummaryMetricCard(
                    modifier = Modifier.weight(1f),
                    value = stats.weekCount.toString(),
                    label = "本周完成"
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stats.latestText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
    )
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.TaskAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Text(
                text = "还没有已完成事项",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "完成后的事项会收纳在这里，方便你回看自己的推进节奏。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CompletedTaskCard(
    task: TaskEntity,
    onRevert: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                task.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                Text(
                    text = "完成于 ${formatTaskTime(task.updatedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                task.dueDate?.let { dueDate ->
                    Text(
                        text = "原计划 ${formatShortDateTime(dueDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onRevert)
            ) {
                Text(
                    text = "已完成",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private data class CompletedStats(
    val totalCount: Int,
    val todayCount: Int,
    val weekCount: Int,
    val latestText: String
)

private data class CompletedGroup(
    val dayStart: Long,
    val title: String,
    val tasks: List<TaskEntity>
)

private fun buildCompletedStats(tasks: List<TaskEntity>): CompletedStats {
    val now = System.currentTimeMillis()
    val todayStart = startOfDay(now)
    val weekStart = startOfWeek(now)

    val totalCount = tasks.size
    val todayCount = tasks.count { it.updatedAt >= todayStart }
    val weekCount = tasks.count { it.updatedAt >= weekStart }

    val latestText = tasks.firstOrNull()?.let {
        "最近一次完成：${formatFullDateTime(it.updatedAt)}"
    } ?: "最近还没有完成记录"

    return CompletedStats(
        totalCount = totalCount,
        todayCount = todayCount,
        weekCount = weekCount,
        latestText = latestText
    )
}

private fun buildCompletedGroups(tasks: List<TaskEntity>): List<CompletedGroup> {
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

private fun formatTaskTime(timestamp: Long): String {
    return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(timestamp))
}

private fun formatShortDateTime(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    return if (hour == 0 && minute == 0) {
        SimpleDateFormat("M月d日", Locale.CHINA).format(Date(timestamp))
    } else {
        SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(timestamp))
    }
}

private fun formatFullDateTime(timestamp: Long): String {
    return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(timestamp))
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

private fun startOfWeek(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        timeInMillis = timestamp
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}