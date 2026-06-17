package com.jimu.app.ui.tasks

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jimu.app.JimuApp
import com.jimu.app.data.local.entity.TaskEntity
import com.jimu.app.ui.completed.buildCompletedGroups
import com.jimu.app.ui.theme.OverdueTint
import com.jimu.app.ui.theme.panelColor
import com.jimu.app.ui.theme.TodayTint
import com.jimu.app.viewmodel.TaskDateOption
import com.jimu.app.viewmodel.TasksViewModel
import com.jimu.app.viewmodel.TasksViewModelFactory
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class TaskViewMode {
    ALL,
    TODAY,
    COMPLETED
}

@Composable
fun TasksScreen(innerPadding: PaddingValues) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as JimuApp
    val context = LocalContext.current

    val viewModel: TasksViewModel = viewModel(
        factory = TasksViewModelFactory(
            repository = app.taskRepository,
            taskReminderScheduler = app.taskReminderScheduler
        )
    )

    val tasks by viewModel.tasks.collectAsState()
    val newTaskCustomTime by viewModel.newTaskCustomTime.collectAsState()
    val editCustomTime by viewModel.editCustomTime.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val newTaskTitle by viewModel.newTaskTitle.collectAsState()
    val newTaskDateOption by viewModel.newTaskDateOption.collectAsState()
    val newTaskCustomDate by viewModel.newTaskCustomDate.collectAsState()

    val editingTask by viewModel.editingTask.collectAsState()
    val editTitle by viewModel.editTitle.collectAsState()
    val editDescription by viewModel.editDescription.collectAsState()
    val editDateOption by viewModel.editDateOption.collectAsState()
    val editCustomDate by viewModel.editCustomDate.collectAsState()

    var viewMode by rememberSaveable { mutableStateOf(TaskViewMode.TODAY) }

    val today = LocalDate.now()
    val zoneId = ZoneId.systemDefault()

    val activeTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    val completedGroups = remember(completedTasks) { buildCompletedGroups(completedTasks) }

    val overdueTasks = activeTasks.filter { task ->
        task.dueDate?.toLocalDate(zoneId)?.isBefore(today) == true
    }

    val todayTasks = activeTasks.filter { task ->
        task.dueDate?.toLocalDate(zoneId) == today
    }

    val plannedTasks = activeTasks.filter { task ->
        task.dueDate?.toLocalDate(zoneId)?.isAfter(today) == true
    }

    val unscheduledTasks = activeTasks.filter { task ->
        task.dueDate == null
    }

    fun showDatePicker(onDatePicked: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                    .atStartOfDay(zoneId)
                    .toInstant()
                    .toEpochMilli()
                onDatePicked(picked)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker(onTimePicked: (Int) -> Unit) {
        val calendar = Calendar.getInstance()
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onTimePicked(hourOfDay * 60 + minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    val editingTaskAllowDate = editingTask?.dueDate?.toLocalDate(zoneId)?.isBefore(today) != true

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openAddDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "新增待办"
                )
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 12.dp
                )
        ) {
            PageHeader(
                title = "待办",
                subtitle = "按时间安排任务，今天没完成的事会继续保留。"
            )

            TaskViewSwitcher(
                currentMode = viewMode,
                onModeChange = { viewMode = it },
                modifier = Modifier.padding(top = 16.dp)
            )

            AnimatedContent(
                targetState = viewMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) +
                            expandVertically(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(180)) +
                            shrinkVertically(animationSpec = tween(180)) using
                            SizeTransform(clip = false)
                },
                label = "task_view_mode"
            ) { targetMode ->
                if (targetMode == TaskViewMode.COMPLETED) {
                    if (completedTasks.isEmpty()) {
                        EmptyState(text = "还没有已完成事项，完成后的待办会收纳在这里。")
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            completedGroups.forEach { group ->
                                item(key = "completed_header_${group.dayStart}") {
                                    TaskGroupHeader(
                                        title = group.title,
                                        subtitle = "点击状态可回退到待办"
                                    )
                                }

                                items(group.tasks, key = { it.id }) { task ->
                                    CompletedTaskItem(
                                        task = task,
                                        onRevert = { viewModel.toggleTaskCompleted(task) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when {
                        activeTasks.isEmpty() -> {
                            EmptyState(text = "还没有待办，点击右下角开始添加。")
                        }

                        targetMode == TaskViewMode.TODAY &&
                                overdueTasks.isEmpty() &&
                                todayTasks.isEmpty() -> {
                            EmptyState(text = "今天没有到期或逾期事项，可以按计划推进。")
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 120.dp)
                            ) {
                                if (overdueTasks.isNotEmpty()) {
                                    item {
                                        TaskGroupHeader(
                                            title = "逾期",
                                            subtitle = "这些事项已经超过原计划日期"
                                        )
                                    }
                                    items(overdueTasks, key = { it.id }) { task ->
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = fadeIn(animationSpec = tween(220)) +
                                                    expandVertically(animationSpec = tween(220)),
                                            exit = fadeOut(animationSpec = tween(180)) +
                                                    shrinkVertically(animationSpec = tween(180))
                                        ) {
                                            TaskItem(
                                                task = task,
                                                showRescheduleMenu = true,
                                                onRescheduleToday = { viewModel.rescheduleTaskToToday(task) },
                                                onRescheduleTomorrow = { viewModel.rescheduleTaskToTomorrow(task) },
                                                onRescheduleCustom = {
                                                    showDatePicker { pickedDate ->
                                                        viewModel.rescheduleTaskToCustomDate(task, pickedDate)
                                                    }
                                                },
                                                onToggleCompleted = { viewModel.toggleTaskCompleted(task) },
                                                onDelete = { viewModel.deleteTask(task) },
                                                onEdit = { viewModel.startEditTask(task) }
                                            )
                                        }
                                    }
                                }

                                if (todayTasks.isNotEmpty()) {
                                    item {
                                        TaskGroupHeader(
                                            title = "今日",
                                            subtitle = "优先处理今天要推进的事项"
                                        )
                                    }
                                    items(todayTasks, key = { it.id }) { task ->
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = fadeIn(animationSpec = tween(220)) +
                                                    expandVertically(animationSpec = tween(220)),
                                            exit = fadeOut(animationSpec = tween(180)) +
                                                    shrinkVertically(animationSpec = tween(180))
                                        ) {
                                            TaskItem(
                                                task = task,
                                                showRescheduleMenu = false,
                                                onRescheduleToday = {},
                                                onRescheduleTomorrow = {},
                                                onRescheduleCustom = {},
                                                onToggleCompleted = { viewModel.toggleTaskCompleted(task) },
                                                onDelete = { viewModel.deleteTask(task) },
                                                onEdit = { viewModel.startEditTask(task) }
                                            )
                                        }
                                    }
                                }

                                if (targetMode == TaskViewMode.ALL && plannedTasks.isNotEmpty()) {
                                    item {
                                        TaskGroupHeader(
                                            title = "计划中",
                                            subtitle = "后续安排的任务"
                                        )
                                    }
                                    items(plannedTasks, key = { it.id }) { task ->
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = fadeIn(animationSpec = tween(220)) +
                                                    expandVertically(animationSpec = tween(220)),
                                            exit = fadeOut(animationSpec = tween(180)) +
                                                    shrinkVertically(animationSpec = tween(180))
                                        ) {
                                            TaskItem(
                                                task = task,
                                                showRescheduleMenu = false,
                                                onRescheduleToday = {},
                                                onRescheduleTomorrow = {},
                                                onRescheduleCustom = {},
                                                onToggleCompleted = { viewModel.toggleTaskCompleted(task) },
                                                onDelete = { viewModel.deleteTask(task) },
                                                onEdit = { viewModel.startEditTask(task) }
                                            )
                                        }
                                    }
                                }

                                if (targetMode == TaskViewMode.ALL && unscheduledTasks.isNotEmpty()) {
                                    item {
                                        TaskGroupHeader(
                                            title = "未安排",
                                            subtitle = "暂时没有指定日期的事项"
                                        )
                                    }
                                    items(unscheduledTasks, key = { it.id }) { task ->
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = fadeIn(animationSpec = tween(220)) +
                                                    expandVertically(animationSpec = tween(220)),
                                            exit = fadeOut(animationSpec = tween(180)) +
                                                    shrinkVertically(animationSpec = tween(180))
                                        ) {
                                            TaskItem(
                                                task = task,
                                                showRescheduleMenu = false,
                                                onRescheduleToday = {},
                                                onRescheduleTomorrow = {},
                                                onRescheduleCustom = {},
                                                onToggleCompleted = { viewModel.toggleTaskCompleted(task) },
                                                onDelete = { viewModel.deleteTask(task) },
                                                onEdit = { viewModel.startEditTask(task) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            title = newTaskTitle,
            dateOption = newTaskDateOption,
            customDate = newTaskCustomDate,
            customTime = newTaskCustomTime,
            onTitleChange = viewModel::onNewTaskTitleChange,
            onDateOptionChange = viewModel::onNewTaskDateOptionChange,
            onPickCustomDate = {
                showDatePicker(viewModel::onNewTaskCustomDateChange)
            },
            onPickCustomTime = {
                showTimePicker(viewModel::onNewTaskCustomTimeChange)
            },
            onDismiss = viewModel::closeAddDialog,
            onSave = viewModel::addTask
        )
    }

    if (editingTask != null) {
        EditTaskDialog(
            title = editTitle,
            description = editDescription,
            dateOption = editDateOption,
            customDate = editCustomDate,
            customTime = editCustomTime,
            allowEditDate = editingTaskAllowDate,
            onTitleChange = viewModel::onEditTitleChange,
            onDescriptionChange = viewModel::onEditDescriptionChange,
            onDateOptionChange = viewModel::onEditDateOptionChange,
            onPickCustomDate = {
                showDatePicker(viewModel::onEditCustomDateChange)
            },
            onPickCustomTime = {
                showTimePicker(viewModel::onEditCustomTimeChange)
            },
            onDismiss = viewModel::cancelEdit,
            onSave = viewModel::saveEdit
        )
    }
}

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(zoneId)
        .toLocalDate()
}

private fun taskDateType(dueDate: Long?, zoneId: ZoneId): String {
    if (dueDate == null) return "UNSCHEDULED"

    val date = dueDate.toLocalDate(zoneId)
    val today = LocalDate.now()

    return when {
        date.isBefore(today) -> "OVERDUE"
        date.isEqual(today) -> "TODAY"
        date.isEqual(today.plusDays(1)) -> "TOMORROW"
        else -> "PLANNED"
    }
}

@Composable
private fun PageHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
private fun TaskViewSwitcher(
    currentMode: TaskViewMode,
    onModeChange: (TaskViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val outerHeight = 52.dp
    val outerPadding = 4.dp
    val spacing = 6.dp
    val tabs = listOf(
        TaskViewMode.TODAY to "今日",
        TaskViewMode.ALL to "全部",
        TaskViewMode.COMPLETED to "已完成"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(outerHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = outerPadding, vertical = outerPadding)
    ) {
        val tabWidth = (maxWidth - spacing * (tabs.size - 1)) / tabs.size
        val selectedIndex = tabs.indexOfFirst { (mode, _) -> mode == currentMode }
            .coerceAtLeast(0)

        val indicatorOffset by animateDpAsState(
            targetValue = (tabWidth + spacing) * selectedIndex.toFloat(),
            animationSpec = tween(
                durationMillis = 460,
                easing = FastOutSlowInEasing
            ),
            label = "task_switch_offset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            tabs.forEach { (mode, label) ->
                SwitcherTab(
                    modifier = Modifier.weight(1f),
                    text = label,
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) }
                )
            }
        }
    }
}

@Composable
private fun SwitcherTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun TaskGroupHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: TaskEntity,
    showRescheduleMenu: Boolean,
    onRescheduleToday: () -> Unit,
    onRescheduleTomorrow: () -> Unit,
    onRescheduleCustom: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val zoneId = ZoneId.systemDefault()
    val dateType = taskDateType(task.dueDate, zoneId)

    val dateChipText = taskDateChipLabel(task.dueDate)
    val timeChipText = taskTimeChipLabel(task.dueDate)

    val dateBadgeBackground = when (dateType) {
        "OVERDUE" -> OverdueTint
        "TODAY" -> TodayTint
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val dateBadgeTextColor = when (dateType) {
        "OVERDUE" -> MaterialTheme.colorScheme.primary
        "TODAY" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = panelColor(isSystemInDarkTheme())),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onToggleCompleted) {
                Icon(
                    imageVector = if (task.isCompleted) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = if (task.isCompleted) "取消完成" else "标记完成",
                    tint = if (task.isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateChipText,
                        style = MaterialTheme.typography.labelSmall,
                        color = dateBadgeTextColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(dateBadgeBackground)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )

                    if (timeChipText != null) {
                        Text(
                            text = timeChipText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (showRescheduleMenu) {
                RescheduleMenu(
                    onRescheduleToday = onRescheduleToday,
                    onRescheduleTomorrow = onRescheduleTomorrow,
                    onRescheduleCustom = onRescheduleCustom
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除"
                )
            }
        }
    }
}

@Composable
private fun CompletedTaskItem(
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
                    text = "记录于 ${formatCompletedRecordTime(task.updatedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                task.dueDate?.let { dueDate ->
                    Text(
                        text = "原计划 ${formatCompletedDueTime(dueDate)}",
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

private fun formatCompletedRecordTime(timestamp: Long): String {
    return SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(timestamp))
}

private fun formatCompletedDueTime(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    return if (hour == 0 && minute == 0) {
        SimpleDateFormat("M月d日", Locale.CHINA).format(Date(timestamp))
    } else {
        SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(timestamp))
    }
}

private fun taskDateChipLabel(dueDate: Long?): String {
    if (dueDate == null) return "未安排"

    val zoneId = ZoneId.systemDefault()
    val due = Instant.ofEpochMilli(dueDate).atZone(zoneId)
    val today = LocalDate.now(zoneId)
    val dueDay = due.toLocalDate()

    return when {
        dueDay.isBefore(today) -> "逾期"
        dueDay.isEqual(today) -> "今天"
        dueDay.isEqual(today.plusDays(1)) -> "明天"
        else -> "${due.monthValue}月${due.dayOfMonth}日"
    }
}

private fun taskTimeChipLabel(dueDate: Long?): String? {
    if (dueDate == null) return null

    val zoneId = ZoneId.systemDefault()
    val due = Instant.ofEpochMilli(dueDate).atZone(zoneId)
    val time = due.toLocalTime()

    return if (time.hour == 0 && time.minute == 0) {
        null
    } else {
        "%02d:%02d".format(time.hour, time.minute)
    }
}

@Composable
private fun RescheduleMenu(
    onRescheduleToday: () -> Unit,
    onRescheduleTomorrow: () -> Unit,
    onRescheduleCustom: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "顺延"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("顺延到今天") },
                onClick = {
                    expanded = false
                    onRescheduleToday()
                }
            )
            DropdownMenuItem(
                text = { Text("顺延到明天") },
                onClick = {
                    expanded = false
                    onRescheduleTomorrow()
                }
            )
            DropdownMenuItem(
                text = { Text("顺延到指定日期") },
                onClick = {
                    expanded = false
                    onRescheduleCustom()
                }
            )
        }
    }
}

@Composable
private fun AddTaskDialog(
    title: String,
    dateOption: TaskDateOption,
    customDate: Long?,
    customTime: Int?,
    onTitleChange: (String) -> Unit,
    onDateOptionChange: (TaskDateOption) -> Unit,
    onPickCustomDate: () -> Unit,
    onPickCustomTime: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增待办") },
        text = {
            TaskDateEditorContent(
                title = title,
                description = null,
                dateOption = dateOption,
                customDate = customDate,
                customTime = customTime,
                onTitleChange = onTitleChange,
                onDescriptionChange = null,
                onDateOptionChange = onDateOptionChange,
                onPickCustomDate = onPickCustomDate,
                onPickCustomTime = onPickCustomTime,
                showDescription = false,
                allowEditDate = true
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditTaskDialog(
    title: String,
    description: String,
    dateOption: TaskDateOption,
    customDate: Long?,
    customTime: Int?,
    allowEditDate: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateOptionChange: (TaskDateOption) -> Unit,
    onPickCustomDate: () -> Unit,
    onPickCustomTime: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑待办") },
        text = {
            TaskDateEditorContent(
                title = title,
                description = description,
                dateOption = dateOption,
                customDate = customDate,
                customTime = customTime,
                onTitleChange = onTitleChange,
                onDescriptionChange = onDescriptionChange,
                onDateOptionChange = onDateOptionChange,
                onPickCustomDate = onPickCustomDate,
                onPickCustomTime = onPickCustomTime,
                showDescription = true,
                allowEditDate = allowEditDate
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun TaskDateEditorContent(
    title: String,
    description: String?,
    dateOption: TaskDateOption,
    customDate: Long?,
    customTime: Int?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: ((String) -> Unit)?,
    onDateOptionChange: (TaskDateOption) -> Unit,
    onPickCustomDate: () -> Unit,
    onPickCustomTime: () -> Unit,
    showDescription: Boolean,
    allowEditDate: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("标题") }
        )

        if (showDescription && onDescriptionChange != null) {
            OutlinedTextField(
                value = description.orEmpty(),
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("描述") },
                minLines = 3
            )
        }

        if (allowEditDate) {
            Text(
                text = "安排时间",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TaskDateOptionRow(
                    label = "不安排",
                    selected = dateOption == TaskDateOption.NONE,
                    onClick = { onDateOptionChange(TaskDateOption.NONE) }
                )
                TaskDateOptionRow(
                    label = "今天",
                    selected = dateOption == TaskDateOption.TODAY,
                    onClick = { onDateOptionChange(TaskDateOption.TODAY) }
                )
                TaskDateOptionRow(
                    label = "明天",
                    selected = dateOption == TaskDateOption.TOMORROW,
                    onClick = { onDateOptionChange(TaskDateOption.TOMORROW) }
                )
                TaskDateOptionRow(
                    label = customDate?.let { "指定日期：${formatCustomDate(it)}" } ?: "指定日期",
                    selected = dateOption == TaskDateOption.CUSTOM,
                    onClick = onPickCustomDate,
                    trailingCalendar = true
                )
            }

            if (dateOption != TaskDateOption.NONE) {
                Spacer(modifier = Modifier.height(4.dp))

                TaskTimeOptionRow(
                    label = customTime?.let { "指定时间：${formatMinutesOfDay(it)}" } ?: "指定时间",
                    onClick = onPickCustomTime
                )
            }
        }
    }
}

private fun formatCustomDate(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return "${date.monthValue}月${date.dayOfMonth}日"
}

@Composable
private fun TaskDateOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailingCalendar: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (trailingCalendar) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = "选择日期",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMinutesOfDay(minutesOfDay: Int): String {
    val hour = minutesOfDay / 60
    val minute = minutesOfDay % 60
    return "%02d:%02d".format(hour, minute)
}

@Composable
private fun TaskTimeOptionRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 48.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = "选择时间",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
