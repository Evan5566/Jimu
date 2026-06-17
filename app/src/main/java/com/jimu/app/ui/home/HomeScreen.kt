package com.jimu.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jimu.app.JimuApp
import com.jimu.app.ui.theme.OceanBlue
import com.jimu.app.viewmodel.HomeCompletionPaceUiModel
import com.jimu.app.viewmodel.HomeGoalFocusUiModel
import com.jimu.app.viewmodel.HomeTodayReviewUiModel
import com.jimu.app.viewmodel.HomeViewModel
import com.jimu.app.viewmodel.HomeViewModelFactory
import com.jimu.app.viewmodel.VoiceInputViewModel
import com.jimu.app.viewmodel.VoiceInputViewModelFactory
import com.jimu.app.voice.AndroidSpeechToTextRepository
import com.jimu.app.voice.MockTaskParseRepository
import com.jimu.app.voice.VoiceInputSheet
import com.jimu.app.voice.VoiceInputTarget

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    resetScrollSignal: Int = 0,
    onOpenReview: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as JimuApp

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            app.taskRepository,
            app.goalRepository,
            app.reviewRepository
        )
    )

    val voiceViewModel: VoiceInputViewModel = viewModel(
        factory = VoiceInputViewModelFactory(
            taskRepository = app.taskRepository,
            habitRepository = app.habitRepository,
            goalRepository = app.goalRepository,
            speechToTextRepository = AndroidSpeechToTextRepository(context),
            taskParseRepository = MockTaskParseRepository()
        )
    )

    val todoCount by homeViewModel.todoCount.collectAsState()
    val completedCount by homeViewModel.completedCount.collectAsState()
    val goalFocus by homeViewModel.goalFocus.collectAsState()
    val todayReview by homeViewModel.todayReview.collectAsState()
    val completionPace = HomeCompletionPaceUiModel.from(
        completedCount = completedCount,
        todoCount = todoCount,
        goalFocus = goalFocus,
        todayReview = todayReview
    )
    val voiceState by voiceViewModel.state.collectAsState()

    var visible by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showVoiceTargetMenu by remember { mutableStateOf(false) }
    var selectedVoiceTarget by remember { mutableStateOf(VoiceInputTarget.TASK) }
    val scrollState = rememberScrollState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceViewModel.startListening(selectedVoiceTarget)
        } else {
            voiceViewModel.setError("未授予录音权限")
        }
    }

    fun startRecordingWithPermission() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            voiceViewModel.startListening(selectedVoiceTarget)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun openVoiceSheet(target: VoiceInputTarget) {
        selectedVoiceTarget = target
        showVoiceTargetMenu = false
        showVoiceSheet = true
        voiceViewModel.reset()
    }

    LaunchedEffect(Unit) {
        visible = true
    }

    LaunchedEffect(resetScrollSignal) {
        if (resetScrollSignal > 0) {
            scrollState.scrollTo(0)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = showVoiceTargetMenu,
                    enter = fadeIn(animationSpec = tween(180)) +
                            slideInVertically(
                                initialOffsetY = { it / 3 },
                                animationSpec = tween(180)
                            ),
                    exit = fadeOut(animationSpec = tween(120))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VoiceTargetChip(
                            text = "待办",
                            enabled = true,
                            onClick = { openVoiceSheet(VoiceInputTarget.TASK) }
                        )
                        VoiceTargetChip(
                            text = "习惯",
                            enabled = true,
                            onClick = { openVoiceSheet(VoiceInputTarget.HABIT) }
                        )
                        VoiceTargetChip(
                            text = "目标",
                            enabled = true,
                            onClick = { openVoiceSheet(VoiceInputTarget.GOAL) }
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        showVoiceTargetMenu = !showVoiceTargetMenu
                        if (!showVoiceTargetMenu) {
                            voiceViewModel.reset()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardVoice,
                        contentDescription = "语音输入"
                    )
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = contentPadding.calculateBottomPadding() + 96.dp
                )
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { -it / 4 },
                            animationSpec = tween(300)
                        )
            ) {
                HomeTopBar()
            }

            Spacer(modifier = Modifier.height(18.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(380)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(380)
                        )
            ) {
                HighlightSummaryCard(
                    title = "今日待处理",
                    value = todoCount.toString(),
                    subtitle = if (todoCount == 0) {
                        "今天没有到期或逾期事项，可以按计划推进。"
                    } else {
                        "今天有 $todoCount 项事项需要优先处理。"
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(450)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(450)
                        )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompletionPaceCard(
                        modifier = Modifier.weight(1f),
                        completionPace = completionPace
                    )

                    GoalFocusCard(
                        modifier = Modifier.weight(1f),
                        goalFocus = goalFocus
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(580)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(580)
                        )
            ) {
                TodayReviewCard(
                    review = todayReview,
                    onClick = onOpenReview
                )
            }
        }

        if (showVoiceSheet) {
            VoiceInputSheet(
                target = selectedVoiceTarget,
                state = voiceState,
                onDismiss = {
                    showVoiceSheet = false
                    voiceViewModel.reset()
                },
                onStartRecording = {
                    startRecordingWithPermission()
                },
                onStopRecording = {
                    voiceViewModel.stopListening()
                },
                onCancelRecording = {
                    voiceViewModel.cancelListening()
                },
                onRetry = {
                    startRecordingWithPermission()
                },
                onConfirmAddTasks = { editedDrafts ->
                    voiceViewModel.addAllTasks(editedDrafts) {
                        showVoiceSheet = false
                    }
                },
                onConfirmAddHabit = { title, description ->
                    voiceViewModel.addHabit(title, description) {
                        showVoiceSheet = false
                    }
                },
                onConfirmAddGoal = { title ->
                    voiceViewModel.addGoal(title) {
                        showVoiceSheet = false
                    }
                }
            )
        }
    }
}

@Composable
private fun TodayReviewCard(
    review: HomeTodayReviewUiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (todayReviewCardContainerStyle(review.hasReview)) {
                TodayReviewCardContainerStyle.Neutral -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日复盘",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = if (review.hasReview) "已记录" else "去记录",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = review.reviewDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (review.hasReview) {
                    review.displaySummary
                } else {
                    "写下今天做得好的事，收个口，也给明天留一个清晰重点。"
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (review.hasReview) 2 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (review.hasReview && review.problems.isNotBlank()) {
                Text(
                    text = "遇到的问题：${review.problems}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (review.hasReview && review.tomorrowFocus.isNotBlank()) {
                Text(
                    text = "明日重点：${review.tomorrowFocus}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

internal enum class TodayReviewCardContainerStyle {
    Neutral
}

internal fun todayReviewCardContainerStyle(hasReview: Boolean): TodayReviewCardContainerStyle {
    return when (hasReview) {
        true -> TodayReviewCardContainerStyle.Neutral
        false -> TodayReviewCardContainerStyle.Neutral
    }
}

@Composable
private fun VoiceTargetChip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = if (enabled) 3.dp else 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = if (enabled) text else "$text·即将支持",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Text(
                    text = "JIMU",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "迹目",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "把重要的事，清晰地推进下去。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun HighlightSummaryCard(
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = OceanBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun CompletionPaceCard(
    modifier: Modifier = Modifier,
    completionPace: HomeCompletionPaceUiModel
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Checklist,
                        contentDescription = completionPace.title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = completionPace.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = completionPace.value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = completionPace.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                completionPace.detailLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalFocusCard(
    modifier: Modifier = Modifier,
    goalFocus: HomeGoalFocusUiModel
) {
    val fullyCompleted = goalFocus.hasGoals &&
            goalFocus.completedGoalCount == goalFocus.goalCount &&
            goalFocus.totalSteps > 0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (fullyCompleted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (fullyCompleted) 4.dp else 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = "目标",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (goalFocus.hasGoals) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = goalFocus.periodLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                text = "当前目标",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (goalFocus.hasGoals) "${goalFocus.progress}%" else "暂无",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = when {
                    !goalFocus.hasGoals -> "先设一个本周或本月目标"
                    fullyCompleted -> "${goalFocus.goalCount} 个目标已全部完成"
                    goalFocus.totalSteps == 0 -> "${goalFocus.goalCount} 个目标，先拆出第一步"
                    else -> "已完成 ${goalFocus.completedSteps}/${goalFocus.totalSteps} 步"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeHintCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary
                ) {}
                Text(
                    text = title,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
