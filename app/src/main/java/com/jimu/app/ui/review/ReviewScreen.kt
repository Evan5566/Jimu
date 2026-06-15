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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jimu.app.JimuApp
import com.jimu.app.viewmodel.ReviewFormUiState
import com.jimu.app.viewmodel.ReviewViewModel
import com.jimu.app.viewmodel.ReviewViewModelFactory

@Composable
fun ReviewScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val app = LocalContext.current.applicationContext as JimuApp
    val viewModel: ReviewViewModel = viewModel(
        factory = ReviewViewModelFactory(app.reviewRepository)
    )

    val uiState by viewModel.uiState.collectAsState()

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
                onBack = onBack
            )

            ReviewEditorCard(
                uiState = uiState,
                onSummaryChange = viewModel::onSummaryChange,
                onProblemsChange = viewModel::onProblemsChange,
                onTomorrowFocusChange = viewModel::onTomorrowFocusChange
            )

            Button(
                onClick = { viewModel.saveReview(onSaved) },
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSaving) "保存中..." else "保存并返回首页")
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
private fun ReviewHeader(
    reviewDate: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "今日复盘",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "今天：$reviewDate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(onClick = onBack) {
            Text("返回")
        }
    }
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
                    text = "正在读取今日复盘...",
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
