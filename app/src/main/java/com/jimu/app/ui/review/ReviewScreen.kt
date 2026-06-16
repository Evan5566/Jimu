package com.jimu.app.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jimu.app.JimuApp
import com.jimu.app.data.repository.DailyDigestUiModel
import com.jimu.app.viewmodel.ReviewFormUiState
import com.jimu.app.viewmodel.ReviewViewModel
import com.jimu.app.viewmodel.ReviewViewModelFactory
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun ReviewScreen(
    innerPadding: PaddingValues,
    reviewDate: String = LocalDate.now().toString(),
    isTopLevelTab: Boolean = false,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val app = LocalContext.current.applicationContext as JimuApp
    val viewModel: ReviewViewModel = viewModel(
        key = "review-$reviewDate",
        factory = ReviewViewModelFactory(
            reviewRepository = app.reviewRepository,
            dailyDigestRepository = app.dailyDigestRepository,
            reviewDate = reviewDate
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val dailyDigest by viewModel.dailyDigest.collectAsState()
    val isTodayReview = uiState.reviewDate == LocalDate.now().toString()
    var showSavedFeedback by remember { mutableStateOf(false) }

    LaunchedEffect(showSavedFeedback) {
        if (showSavedFeedback) {
            delay(1800)
            showSavedFeedback = false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = contentPadding.calculateTopPadding() + 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReviewHeader(
                reviewDate = uiState.reviewDate,
                onOpenHistory = onOpenHistory,
                onBack = onBack,
                showBackButton = reviewShowBackButton(isTopLevelTab)
            )

            if (isTodayReview) {
                DailyDigestCard(dailyDigest = dailyDigest)
            }

            ReviewEditorCard(
                uiState = uiState,
                onSummaryChange = {
                    showSavedFeedback = false
                    viewModel.onSummaryChange(it)
                },
                onProblemsChange = {
                    showSavedFeedback = false
                    viewModel.onProblemsChange(it)
                },
                onTomorrowFocusChange = {
                    showSavedFeedback = false
                    viewModel.onTomorrowFocusChange(it)
                }
            )

            Button(
                onClick = {
                    showSavedFeedback = false
                    viewModel.saveReview {
                        if (isTopLevelTab) {
                            showSavedFeedback = true
                        }
                        onSaved()
                    }
                },
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    reviewSaveButtonText(
                        isSaving = uiState.isSaving,
                        isTopLevelTab = isTopLevelTab
                    )
                )
            }

            if (showSavedFeedback) {
                Text(
                    text = "已保存",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!uiState.isLoading && uiState.summary.isBlank()) {
                Text(
                    text = "至少写下“做得好的事”后才能保存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiState.saveError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DailyDigestCard(
    dailyDigest: DailyDigestUiModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = dailyDigest.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dailyDigest.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dailyDigest.summaryLines.forEach { line ->
                    Text(
                        text = "· $line",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewHeader(
    reviewDate: String,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit,
    showBackButton: Boolean
) {
    val isToday = reviewDate == LocalDate.now().toString()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (isToday) "今日复盘" else "复盘记录",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isToday) "今天：$reviewDate" else "日期：$reviewDate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row {
            TextButton(onClick = onOpenHistory) {
                Text("历史")
            }
            if (showBackButton) {
                TextButton(onClick = onBack) {
                    Text("返回")
                }
            }
        }
    }
}

internal fun reviewShowBackButton(isTopLevelTab: Boolean): Boolean {
    return !isTopLevelTab
}

internal fun reviewSaveButtonText(
    isSaving: Boolean,
    isTopLevelTab: Boolean
): String {
    if (isSaving) return "保存中..."
    return if (isTopLevelTab) "保存" else "保存并返回"
}

@Composable
private fun ReviewEditorCard(
    uiState: ReviewFormUiState,
    onSummaryChange: (String) -> Unit,
    onProblemsChange: (String) -> Unit,
    onTomorrowFocusChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.isLoading) {
                Text(
                    text = "正在读取复盘...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = uiState.summary,
                onValueChange = onSummaryChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && !uiState.isSaving,
                label = { Text("做得好的事") },
                minLines = 4,
                maxLines = 6
            )

            OutlinedTextField(
                value = uiState.problems,
                onValueChange = onProblemsChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && !uiState.isSaving,
                label = { Text("遇到的问题") },
                minLines = 4,
                maxLines = 6
            )

            OutlinedTextField(
                value = uiState.tomorrowFocus,
                onValueChange = onTomorrowFocusChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && !uiState.isSaving,
                label = { Text("明日重点") },
                minLines = 4,
                maxLines = 6
            )
        }
    }
}
