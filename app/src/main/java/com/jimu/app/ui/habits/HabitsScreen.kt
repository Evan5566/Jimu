package com.jimu.app.ui.habits

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jimu.app.JimuApp
import com.jimu.app.data.repository.HabitUiModel
import com.jimu.app.ui.theme.PanelBlue
import com.jimu.app.viewmodel.HabitDialogMode
import com.jimu.app.viewmodel.HabitTab
import com.jimu.app.viewmodel.HabitSummaryUiState
import com.jimu.app.viewmodel.HabitsViewModel
import com.jimu.app.viewmodel.HabitsViewModelFactory

@Composable
fun HabitsScreen(innerPadding: PaddingValues) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as JimuApp
    val viewModel: HabitsViewModel = viewModel(
        factory = HabitsViewModelFactory(app.habitRepository)
    )

    val selectedTab by viewModel.selectedTab.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val pendingHabits by viewModel.pendingHabits.collectAsState()
    val completedHabits by viewModel.completedHabits.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val isEmpty by viewModel.isEmpty.collectAsState()

    val dialogMode by viewModel.dialogMode.collectAsState()
    val draftName by viewModel.draftName.collectAsState()
    val draftDescription by viewModel.draftDescription.collectAsState()

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openAddDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "新增习惯"
                )
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            PageHeader(
                title = "习惯",
                subtitle = "让重复的小事，慢慢变成稳定的节奏。"
            )

            if (isEmpty) {
                EmptyState(
                    title = "还没有习惯",
                    subtitle = "点击右下角开始添加第一个习惯。"
                )
            } else {
                HabitSummaryCard(
                    summary = summary,
                    modifier = Modifier.padding(top = 16.dp)
                )

                HabitViewSwitcher(
                    currentTab = selectedTab,
                    onTabChange = viewModel::selectTab,
                    modifier = Modifier.padding(top = 16.dp)
                )

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) +
                                expandVertically(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(180)) +
                                shrinkVertically(animationSpec = tween(180)) using
                                SizeTransform(clip = false)
                    },
                    label = "habit_tab_content"
                ) { targetTab ->
                    LazyColumn(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        when (targetTab) {
                            HabitTab.TODAY -> {
                                if (pendingHabits.isNotEmpty()) {
                                    item {
                                        HabitGroupHeader(
                                            title = "进行中",
                                            subtitle = "今天还没有完成的习惯"
                                        )
                                    }
                                    items(pendingHabits, key = { it.habit.id }) { habitUi ->
                                        HabitItem(
                                            habitUi = habitUi,
                                            onCheckIn = { viewModel.checkInToday(habitUi.habit) },
                                            onEdit = { viewModel.openEditDialog(habitUi.habit) },
                                            onDelete = { viewModel.deleteHabit(habitUi.habit) }
                                        )
                                    }
                                }

                                if (completedHabits.isNotEmpty()) {
                                    item {
                                        HabitGroupHeader(
                                            title = "今日已完成",
                                            subtitle = "已经打卡的习惯"
                                        )
                                    }
                                    items(completedHabits, key = { it.habit.id }) { habitUi ->
                                        HabitItem(
                                            habitUi = habitUi,
                                            onCheckIn = { viewModel.checkInToday(habitUi.habit) },
                                            onEdit = { viewModel.openEditDialog(habitUi.habit) },
                                            onDelete = { viewModel.deleteHabit(habitUi.habit) }
                                        )
                                    }
                                }

                                if (pendingHabits.isEmpty() && completedHabits.isNotEmpty()) {
                                    item {
                                        SmallTipCard(text = "今天的习惯已经全部完成，继续保持。")
                                    }
                                }

                                if (pendingHabits.isEmpty() && completedHabits.isEmpty()) {
                                    item {
                                        SmallTipCard(text = "今天还没有习惯，去全部里看看吧。")
                                    }
                                }
                            }

                            HabitTab.ALL -> {
                                item {
                                    HabitGroupHeader(
                                        title = "全部习惯",
                                        subtitle = "未完成的排在前面，更方便今天直接处理"
                                    )
                                }

                                items(allHabits, key = { it.habit.id }) { habitUi ->
                                    HabitItem(
                                        habitUi = habitUi,
                                        onCheckIn = { viewModel.checkInToday(habitUi.habit) },
                                        onEdit = { viewModel.openEditDialog(habitUi.habit) },
                                        onDelete = { viewModel.deleteHabit(habitUi.habit) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (dialogMode != null) {
        HabitEditorDialog(
            mode = dialogMode!!,
            name = draftName,
            description = draftDescription,
            onNameChange = viewModel::onDraftNameChange,
            onDescriptionChange = viewModel::onDraftDescriptionChange,
            onDismiss = viewModel::closeDialog,
            onConfirm = viewModel::submitDialog
        )
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
private fun HabitSummaryCard(
    summary: HabitSummaryUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "今日进度",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    value = "${summary.completedCount}/${summary.totalCount}",
                    label = "已完成"
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    value = "${summary.remainingCount}",
                    label = "待完成"
                )
                SummaryMetric(
                    modifier = Modifier.weight(1f),
                    value = "${summary.totalCheckIns}",
                    label = "累计打卡"
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HabitViewSwitcher(
    currentTab: HabitTab,
    onTabChange: (HabitTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val outerHeight = 52.dp
    val outerPadding = 4.dp
    val spacing = 6.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(outerHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = outerPadding, vertical = outerPadding)
    ) {
        val tabWidth = (maxWidth - spacing) / 2

        val indicatorOffset by animateDpAsState(
            targetValue = if (currentTab == HabitTab.TODAY) 0.dp else tabWidth + spacing,
            animationSpec = tween(
                durationMillis = 460,
                easing = FastOutSlowInEasing
            ),
            label = "habit_switch_offset"
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
            SwitcherTab(
                modifier = Modifier.weight(1f),
                text = "今日",
                selected = currentTab == HabitTab.TODAY,
                onClick = { onTabChange(HabitTab.TODAY) }
            )
            SwitcherTab(
                modifier = Modifier.weight(1f),
                text = "全部",
                selected = currentTab == HabitTab.ALL,
                onClick = { onTabChange(HabitTab.ALL) }
            )
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
private fun HabitGroupHeader(
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
private fun EmptyState(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 72.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmallTipCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitItem(
    habitUi: HabitUiModel,
    onCheckIn: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onCheckIn,
                enabled = !habitUi.checkedToday,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = if (habitUi.checkedToday) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = if (habitUi.checkedToday) "今天已打卡" else "今日打卡",
                    tint = if (habitUi.checkedToday) {
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
                    text = habitUi.habit.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                val descriptionText = habitUi.habit.description?.takeIf { it.isNotBlank() }
                if (descriptionText != null) {
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = if (habitUi.checkedToday) {
                        "今天已打卡，继续保持这个节奏。"
                    } else {
                        "今天未打卡，完成后会记录一次。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HabitChip(
                        label = "状态",
                        value = if (habitUi.checkedToday) "今日已完成" else "待打卡"
                    )
                    HabitChip(
                        label = "连续",
                        value = "${habitUi.streakCount} 天"
                    )
                    HabitChip(
                        label = "累计",
                        value = "${habitUi.totalCount} 次"
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "编辑"
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
}

@Composable
private fun HabitChip(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HabitEditorDialog(
    mode: HabitDialogMode,
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (mode == HabitDialogMode.ADD) "新增习惯" else "编辑习惯"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("习惯名称") },
                    placeholder = { Text("例如：喝水、背单词、早睡") }
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    label = { Text("备注（可选）") },
                    placeholder = { Text("例如：每天至少喝 2L 水") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(if (mode == HabitDialogMode.ADD) "添加" else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}