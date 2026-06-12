package com.jimu.app.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputSheet(
    target: VoiceInputTarget,
    state: VoiceInputState,
    onDismiss: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onRetry: () -> Unit,
    onConfirmAddTasks: (List<TaskDraft>) -> Unit,
    onConfirmAddHabit: (String, String) -> Unit,
    onConfirmAddGoal: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (target) {
                                VoiceInputTarget.TASK -> Icons.Outlined.Checklist
                                VoiceInputTarget.HABIT -> Icons.Outlined.Loop
                                VoiceInputTarget.GOAL -> Icons.Outlined.Flag
                            },
                            contentDescription = target.label,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column {
                    Text(
                        text = "语音新增${target.label}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = targetSubtitle(target),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (state) {
                VoiceInputState.Idle -> IdleContent(
                    target = target,
                    onStartRecording = onStartRecording
                )

                VoiceInputState.RequestingPermission -> {
                    LoadingCard("正在请求权限...", "请允许访问麦克风")
                }

                is VoiceInputState.Recording -> {
                    RecordingContent(
                        partialText = state.partialText,
                        onStopRecording = onStopRecording,
                        onCancelRecording = onCancelRecording
                    )
                }

                VoiceInputState.Processing -> {
                    LoadingCard("正在整理识别结果...", "马上就好")
                }

                is VoiceInputState.TaskReview -> {
                    TaskReviewContent(
                        transcript = state.transcript,
                        taskDrafts = state.taskDrafts,
                        onRetry = onRetry,
                        onConfirmAdd = onConfirmAddTasks
                    )
                }

                is VoiceInputState.HabitReview -> {
                    HabitReviewContent(
                        target = target,
                        transcript = state.transcript,
                        habitDraft = state.habitDraft,
                        onRetry = onRetry,
                        onConfirmAddHabit = onConfirmAddHabit,
                        onConfirmAddGoal = onConfirmAddGoal
                    )
                }

                is VoiceInputState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    target: VoiceInputTarget,
    onStartRecording: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = "语音输入",
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (target) {
                    VoiceInputTarget.TASK -> "说出要完成的事"
                    VoiceInputTarget.HABIT -> "说出想坚持的习惯"
                    VoiceInputTarget.GOAL -> "说出你想达成的目标"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = targetExample(target),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onStartRecording,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardVoice,
                    contentDescription = null
                )
                Text("开始识别", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun RecordingContent(
    partialText: String,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardVoice,
                contentDescription = "识别中",
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "正在听你说",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = partialText.ifBlank { "请开始说话…" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }

                Button(
                    onClick = onStopRecording,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("停止")
                }
            }
        }
    }
}

@Composable
private fun LoadingCard(
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskReviewContent(
    transcript: String,
    taskDrafts: List<TaskDraft>,
    onRetry: () -> Unit,
    onConfirmAdd: (List<TaskDraft>) -> Unit
) {
    val editableDrafts = remember { mutableStateListOf<TaskDraft>() }

    LaunchedEffect(taskDrafts) {
        editableDrafts.clear()
        editableDrafts.addAll(taskDrafts)
    }

    SectionLabel("识别文本")
    Spacer(modifier = Modifier.height(10.dp))
    TranscriptCard(transcript)

    Spacer(modifier = Modifier.height(18.dp))

    SectionLabel("确认待办")
    Spacer(modifier = Modifier.height(10.dp))

    editableDrafts.forEachIndexed { index, task ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "待办 ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = task.title,
                    onValueChange = { newTitle ->
                        editableDrafts[index] = task.copy(title = newTitle)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("标题") }
                )

                task.dueDateMillis?.let { due ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "时间：${formatDueDate(due)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.weight(1f)
        ) {
            Text("重新识别")
        }

        Button(
            onClick = { onConfirmAdd(editableDrafts.toList()) },
            modifier = Modifier.weight(1f),
            enabled = editableDrafts.isNotEmpty()
        ) {
            Text("添加待办")
        }
    }
}

@Composable
private fun HabitReviewContent(
    target: VoiceInputTarget,
    transcript: String,
    habitDraft: HabitDraft,
    onRetry: () -> Unit,
    onConfirmAddHabit: (String, String) -> Unit,
    onConfirmAddGoal: (String) -> Unit
) {
    var title by remember(habitDraft) { mutableStateOf(habitDraft.title) }
    var description by remember(habitDraft) { mutableStateOf(habitDraft.description) }

    val isGoal = target == VoiceInputTarget.GOAL

    SectionLabel("识别文本")
    Spacer(modifier = Modifier.height(10.dp))
    TranscriptCard(transcript)

    Spacer(modifier = Modifier.height(18.dp))

    SectionLabel(if (isGoal) "确认目标" else "确认习惯")
    Spacer(modifier = Modifier.height(10.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(if (isGoal) "目标名称" else "习惯名称") }
            )

            if (!isGoal) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    label = { Text("备注（可选）") },
                    placeholder = { Text("例如：每天至少喝 2L 水") }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.weight(1f)
        ) {
            Text("重新识别")
        }

        Button(
            onClick = {
                if (isGoal) {
                    onConfirmAddGoal(title)
                } else {
                    onConfirmAddHabit(title, description)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = title.trim().isNotBlank()
        ) {
            Text(if (isGoal) "添加目标" else "添加习惯")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun TranscriptCard(transcript: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = transcript,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "出了点问题",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重新尝试")
            }
        }
    }
}

private fun targetSubtitle(target: VoiceInputTarget): String {
    return when (target) {
        VoiceInputTarget.TASK -> "把想到的事整理成待办"
        VoiceInputTarget.HABIT -> "把想坚持的内容整理成习惯"
        VoiceInputTarget.GOAL -> "把想完成的方向整理成目标"
    }
}

private fun targetExample(target: VoiceInputTarget): String {
    return when (target) {
        VoiceInputTarget.TASK -> "例如：明天下午三点开会，晚上交报告。"
        VoiceInputTarget.HABIT -> "例如：每天喝水，晚上背单词。"
        VoiceInputTarget.GOAL -> "例如：这个月读完 2 本专业书。"
    }
}

private fun formatDueDate(timeMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}