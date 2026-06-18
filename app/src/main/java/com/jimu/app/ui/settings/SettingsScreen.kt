package com.jimu.app.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jimu.app.JimuApp
import com.jimu.app.data.backup.BackupFileReadResult
import com.jimu.app.data.backup.BackupFileWriteResult
import com.jimu.app.data.backup.BackupStreamIo
import com.jimu.app.viewmodel.SettingsUiState
import com.jimu.app.viewmodel.SettingsViewModel
import com.jimu.app.viewmodel.SettingsViewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as JimuApp
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            backupRepository = app.backupRepository,
            reminderRebuilder = app.backupReminderRebuilder,
            appPackage = context.packageName,
            appVersionName = packageInfo.versionName.orEmpty(),
            appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val state = viewModel.uiState.value as? SettingsUiState.WaitingForExportLocation
        if (uri == null || state == null) {
            viewModel.onExportLocationCancelled()
        } else {
            viewModel.onExportLocationSelected()
            scope.launch {
                viewModel.onExportWritten(writeBackup(context, uri, state.json))
            }
        }
    }

    val preRestoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val state = viewModel.uiState.value as?
            SettingsUiState.WaitingForPreImportBackupLocation
        if (uri == null || state == null) {
            viewModel.onPreImportBackupLocationCancelled()
        } else {
            viewModel.onPreImportBackupLocationSelected()
            scope.launch {
                viewModel.onPreImportBackupWritten(
                    writeBackup(context, uri, state.json)
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            viewModel.onImportPickerCancelled()
        } else {
            scope.launch {
                viewModel.onImportFileRead(readBackup(context, uri))
            }
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SettingsUiState.WaitingForExportLocation -> {
                exportLauncher.launch(state.fileName)
            }
            is SettingsUiState.WaitingForPreImportBackupLocation -> {
                preRestoreBackupLauncher.launch(state.fileName)
            }
            else -> Unit
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
            SettingsHeader(onBack = onBack)

            SettingsCard(title = "版本信息") {
                Text(
                    text = "迹目 ${packageInfo.versionName.orEmpty()} " +
                        "(${PackageInfoCompat.getLongVersionCode(packageInfo)})",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            SettingsCard(title = "本地数据备份") {
                Text(
                    text = "备份文件为未加密明文，可能包含待办、目标和复盘内容，请妥善保管。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = viewModel::requestExport,
                    enabled = !uiState.isBusy(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("导出数据备份")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.onImportPickerOpened()
                        importLauncher.launch(
                            arrayOf("application/json", "text/json", "text/plain")
                        )
                    },
                    enabled = !uiState.isBusy(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("从备份恢复")
                }
            }

            SettingsStatus(uiState = uiState, onDismiss = viewModel::clearResult)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val preview = uiState as? SettingsUiState.PreviewReady
    if (preview != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelRestorePreview,
            title = { Text("确认恢复数据") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("恢复会替换当前数据；恢复前会先要求保存当前数据备份。")
                    Text("备份时间：${formatBackupTime(preview.preview.exportedAt)}")
                    Text(
                        "备份内容：待办 ${preview.preview.tasks}，习惯 " +
                            "${preview.preview.habits}，打卡 ${preview.preview.habitRecords}，" +
                            "目标 ${preview.preview.goals}，步骤 ${preview.preview.goalSteps}，" +
                            "复盘 ${preview.preview.reviews}。"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) {
                    Text("继续恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestorePreview) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onBack) {
            Text("返回")
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun SettingsStatus(
    uiState: SettingsUiState,
    onDismiss: () -> Unit
) {
    val status = when (uiState) {
        SettingsUiState.Exporting -> "正在生成备份..."
        SettingsUiState.WritingExport -> "正在写入备份..."
        SettingsUiState.ReadingImportFile -> "正在读取并校验备份..."
        SettingsUiState.PreparingPreImportBackup -> "正在生成恢复前保险备份..."
        is SettingsUiState.SavingPreImportBackup -> "正在保存恢复前保险备份..."
        SettingsUiState.RestoringDatabase -> "正在恢复数据..."
        SettingsUiState.RebuildingReminders -> "正在重建待办提醒..."
        else -> null
    }

    if (status != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator()
            Text(status)
        }
    }

    when (uiState) {
        is SettingsUiState.Success -> ResultCard(
            message = uiState.message,
            isError = false,
            onDismiss = onDismiss
        )
        is SettingsUiState.PartialSuccess -> ResultCard(
            message = uiState.message,
            isError = true,
            onDismiss = onDismiss
        )
        is SettingsUiState.Error -> ResultCard(
            message = uiState.message,
            isError = true,
            onDismiss = onDismiss
        )
        else -> Unit
    }
}

@Composable
private fun ResultCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    }
}

private fun SettingsUiState.isBusy(): Boolean {
    return this !is SettingsUiState.Idle &&
        this !is SettingsUiState.Success &&
        this !is SettingsUiState.PartialSuccess &&
        this !is SettingsUiState.Error &&
        this !is SettingsUiState.PreviewReady
}

private suspend fun readBackup(
    context: Context,
    uri: Uri
): BackupFileReadResult = withContext(Dispatchers.IO) {
    try {
        val declaredSize = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            BackupStreamIo.readUtf8Limited(input, declaredSize)
        } ?: BackupFileReadResult.ReadFailed("无法打开备份文件")
    } catch (error: Exception) {
        BackupFileReadResult.ReadFailed(error.message ?: "读取备份文件失败")
    }
}

private suspend fun writeBackup(
    context: Context,
    uri: Uri,
    json: String
): BackupFileWriteResult = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            BackupStreamIo.writeUtf8(output, json)
        } ?: BackupFileWriteResult.WriteFailed("无法创建备份文件")
    } catch (error: Exception) {
        BackupFileWriteResult.WriteFailed(error.message ?: "写入备份文件失败")
    }
}

private fun formatBackupTime(exportedAt: Long): String {
    return runCatching {
        Instant.ofEpochMilli(exportedAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }.getOrDefault("未知")
}
