package com.jimu.app.ui.goals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jimu.app.JimuApp
import com.jimu.app.data.local.entity.GoalEntity
import com.jimu.app.data.local.entity.GoalStepEntity
import com.jimu.app.data.repository.GoalOverviewUiModel
import com.jimu.app.data.repository.GoalUiModel
import com.jimu.app.ui.theme.OceanBlue
import com.jimu.app.viewmodel.GoalsViewModel
import com.jimu.app.viewmodel.GoalsViewModelFactory

@Composable
fun GoalsScreen(innerPadding: PaddingValues) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as JimuApp
    val viewModel: GoalsViewModel = viewModel(
        factory = GoalsViewModelFactory(app.goalRepository)
    )

    val goals by viewModel.goals.collectAsState()
    val overview by viewModel.overview.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val newGoalTitle by viewModel.newGoalTitle.collectAsState()
    val stepInputs by viewModel.stepInputs.collectAsState()
    val editingGoal by viewModel.editingGoal.collectAsState()
    val editingGoalTitle by viewModel.editingGoalTitle.collectAsState()
    val editingStep by viewModel.editingStep.collectAsState()
    val editingStepTitle by viewModel.editingStepTitle.collectAsState()

    val groupedGoals = remember(goals) { buildGoalSections(goals) }

    var pendingDeleteGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var pendingDeleteStep by remember { mutableStateOf<GoalStepEntity?>(null) }
    var pendingRestoreGoal by remember { mutableStateOf<GoalEntity?>(null) }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openAddDialog) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "新增目标"
                )
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PageHeader(
                    title = "目标",
                    subtitle = "把长期计划拆成步骤，一步步推进。"
                )
            }

            item {
                GoalOverviewSection(overview = overview)
            }

            if (goals.isEmpty()) {
                item {
                    EmptyState(text = "还没有目标，点击右下角开始添加。")
                }
            } else {
                groupedGoals.forEach { section ->
                    item(key = "section_${section.type.name}") {
                        GoalSectionHeader(
                            title = section.type.displayName,
                            count = section.goals.size
                        )
                    }

                    items(
                        items = section.goals,
                        key = { it.goal.id }
                    ) { goalUi ->
                        GoalItem(
                            goalUi = goalUi,
                            periodType = section.type,
                            stepInput = stepInputs[goalUi.goal.id].orEmpty(),
                            onStepInputChange = { viewModel.onStepInputChange(goalUi.goal.id, it) },
                            onAddStep = { viewModel.addGoalStep(goalUi.goal.id) },
                            onEditGoal = { viewModel.openEditGoalDialog(goalUi.goal) },
                            onToggleStep = viewModel::toggleGoalStep,
                            onEditStep = viewModel::openEditStepDialog,
                            onRestoreGoal = { pendingRestoreGoal = goalUi.goal },
                            onDeleteGoal = { pendingDeleteGoal = goalUi.goal },
                            onDeleteStep = { pendingDeleteStep = it }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            title = newGoalTitle,
            onTitleChange = viewModel::onNewGoalTitleChange,
            onDismiss = viewModel::closeAddDialog,
            onSave = viewModel::addGoal
        )
    }

    if (editingGoal != null) {
        EditTextDialog(
            dialogTitle = "编辑目标",
            fieldLabel = "目标名称",
            value = editingGoalTitle,
            confirmText = "保存",
            onValueChange = viewModel::onEditingGoalTitleChange,
            onDismiss = viewModel::closeEditGoalDialog,
            onConfirm = viewModel::saveGoalEdit
        )
    }

    if (editingStep != null) {
        EditTextDialog(
            dialogTitle = "编辑步骤",
            fieldLabel = "步骤内容",
            value = editingStepTitle,
            confirmText = "保存",
            onValueChange = viewModel::onEditingStepTitleChange,
            onDismiss = viewModel::closeEditStepDialog,
            onConfirm = viewModel::saveStepEdit
        )
    }

    pendingRestoreGoal?.let { goal ->
        ConfirmActionDialog(
            title = "恢复目标",
            content = "将把该目标下的步骤恢复为未完成状态。",
            confirmText = "恢复",
            onConfirm = {
                viewModel.restoreGoal(goal)
                pendingRestoreGoal = null
            },
            onDismiss = { pendingRestoreGoal = null }
        )
    }

    pendingDeleteGoal?.let { goal ->
        ConfirmDeleteDialog(
            title = "删除目标",
            content = "删除后，这个目标及其所有步骤都会移除。",
            onConfirm = {
                viewModel.deleteGoal(goal)
                pendingDeleteGoal = null
            },
            onDismiss = { pendingDeleteGoal = null }
        )
    }

    pendingDeleteStep?.let { step ->
        ConfirmDeleteDialog(
            title = "删除步骤",
            content = "确认删除“${step.title}”吗？",
            onConfirm = {
                viewModel.deleteGoalStep(step)
                pendingDeleteStep = null
            },
            onDismiss = { pendingDeleteStep = null }
        )
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
private fun GoalOverviewSection(overview: GoalOverviewUiModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = OceanBlue),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "整体推进",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = "${overview.progress}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                LinearProgressIndicator(
                    progress = { overview.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.24f)
                )

                Text(
                    text = "已完成 ${overview.completedSteps} / ${overview.totalSteps} 个步骤",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewSmallCard(
                modifier = Modifier.weight(1f),
                title = "目标数",
                value = overview.totalGoals.toString(),
                subtitle = "已完成 ${overview.completedGoals} 个"
            )
            OverviewSmallCard(
                modifier = Modifier.weight(1f),
                title = "步骤数",
                value = overview.totalSteps.toString(),
                subtitle = if (overview.totalSteps == 0) {
                    "先拆出第一步"
                } else {
                    "已完成 ${overview.completedSteps} 个"
                }
            )
        }
    }
}

@Composable
private fun OverviewSmallCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalSectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            Text(
                text = "$count",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalItem(
    goalUi: GoalUiModel,
    periodType: GoalPeriodType,
    stepInput: String,
    onStepInputChange: (String) -> Unit,
    onAddStep: () -> Unit,
    onEditGoal: () -> Unit,
    onToggleStep: (GoalStepEntity) -> Unit,
    onEditStep: (GoalStepEntity) -> Unit,
    onRestoreGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
    onDeleteStep: (GoalStepEntity) -> Unit
) {
    var showStepEditor by rememberSaveable(goalUi.goal.id) { mutableStateOf(false) }
    val displayTitle = remember(goalUi.goal.title, periodType) {
        stripPeriodPrefix(goalUi.goal.title, periodType)
    }
    val completed = goalUi.isCompleted

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (completed) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (periodType != GoalPeriodType.OTHER) {
                            PeriodChip(periodType.displayName)
                        }

                        if (completed) {
                            GoalInfoChip(
                                text = "已完成",
                                highlighted = true,
                                onClick = onRestoreGoal
                            )
                        } else {
                            GoalInfoChip(text = "完成 ${goalUi.progress}%")
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEditGoal) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "编辑目标",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteGoal) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "删除目标"
                        )
                    }
                }
            }

            if (completed) {
                Text(
                    text = "${goalUi.completedStepCount} / ${goalUi.totalStepCount} 步已完成",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    progress = { goalUi.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .height(8.dp)
                )

                Text(
                    text = if (goalUi.totalStepCount == 0) {
                        "还没有拆分步骤，先补上第一步。"
                    } else {
                        "已完成 ${goalUi.completedStepCount} / ${goalUi.totalStepCount} 步"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (goalUi.steps.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                    ) {
                        Text(
                            text = "先把这个目标拆成 2~5 个可执行步骤，推进会更清晰。",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goalUi.steps.forEach { step ->
                            GoalStepItem(
                                step = step,
                                onToggle = { onToggleStep(step) },
                                onEdit = { onEditStep(step) },
                                onDelete = { onDeleteStep(step) }
                            )
                        }
                    }
                }

                AddStepSection(
                    expanded = showStepEditor,
                    value = stepInput,
                    onExpand = { showStepEditor = true },
                    onValueChange = onStepInputChange,
                    onCancel = {
                        onStepInputChange("")
                        showStepEditor = false
                    },
                    onConfirm = {
                        if (stepInput.trim().isNotBlank()) {
                            onAddStep()
                            showStepEditor = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AddStepSection(
    expanded: Boolean,
    value: String,
    onExpand: () -> Unit,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AnimatedVisibility(
        visible = !expanded,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onExpand),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "添加步骤",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("步骤内容") },
                    placeholder = { Text("例如：先列出本月阅读书单") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text("取消")
                    }

                    FilledTonalButton(
                        onClick = onConfirm,
                        enabled = value.trim().isNotBlank()
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun GoalInfoChip(
    text: String,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        },
        shape = RoundedCornerShape(999.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun GoalStepItem(
    step: GoalStepEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val completed = step.isCompleted

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (completed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (completed) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = if (completed) "取消完成" else "标记完成",
                    tint = if (completed) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = step.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (completed) TextDecoration.LineThrough else null
            )

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "编辑步骤",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除步骤",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddGoalDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增目标") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("输入目标名称") }
                )
                Text(
                    text = "建议使用清晰结果导向的命名，比如：本月读完 2 本专业书。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = title.trim().isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun EditTextDialog(
    dialogTitle: String,
    fieldLabel: String,
    value: String,
    confirmText: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(fieldLabel) }
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = value.trim().isNotBlank()
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    content: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(content) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    content: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(content) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private enum class GoalPeriodType(val displayName: String) {
    WEEK("本周"),
    MONTH("本月"),
    YEAR("本年"),
    OTHER("其他")
}

private data class GoalSection(
    val type: GoalPeriodType,
    val goals: List<GoalUiModel>
)

private fun buildGoalSections(goals: List<GoalUiModel>): List<GoalSection> {
    val order = listOf(
        GoalPeriodType.WEEK,
        GoalPeriodType.MONTH,
        GoalPeriodType.YEAR,
        GoalPeriodType.OTHER
    )

    return order.mapNotNull { type ->
        val filtered = goals.filter { detectGoalPeriod(it.goal.title) == type }
        if (filtered.isEmpty()) null else GoalSection(type, filtered)
    }
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

private fun stripPeriodPrefix(
    title: String,
    periodType: GoalPeriodType
): String {
    val prefixes = when (periodType) {
        GoalPeriodType.WEEK -> listOf("本周", "这周", "这个星期", "这星期", "这一周", "当周")
        GoalPeriodType.MONTH -> listOf("本月", "这个月", "这月", "当月", "这一个月")
        GoalPeriodType.YEAR -> listOf("本年", "今年", "这一年", "当年")
        GoalPeriodType.OTHER -> emptyList()
    }

    val normalized = title.trim()
    val stripped = prefixes.firstNotNullOfOrNull { prefix ->
        normalized.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.trim()
    }

    return stripped?.takeIf { it.isNotBlank() } ?: normalized
}