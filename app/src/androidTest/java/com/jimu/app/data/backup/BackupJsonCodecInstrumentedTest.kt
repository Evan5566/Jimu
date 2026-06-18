package com.jimu.app.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupJsonCodecInstrumentedTest {

    @Test
    fun androidJsonImplementationRoundTripsBackupV1() {
        val payload = instrumentedPayload(taskId = 1L, title = "设备端 JSON")

        val result = BackupJsonCodec.decode(BackupJsonCodec.encode(payload))

        assertTrue(result is BackupDecodeResult.Success)
        assertEquals(payload, (result as BackupDecodeResult.Success).payload)
    }
}

internal fun instrumentedPayload(
    taskId: Long,
    title: String
): AppBackupPayloadV1 {
    return AppBackupPayloadV1(
        meta = BackupMetaV1(
            exportedAt = 1_800_000_000_000L,
            appPackage = BACKUP_APP_PACKAGE,
            appVersionName = "1.0",
            appVersionCode = 1L
        ),
        tasks = listOf(
            TaskBackupV1(
                id = taskId,
                title = title,
                description = null,
                dueDate = null,
                isCompleted = false,
                createdAt = 10L,
                updatedAt = 10L
            )
        ),
        habits = emptyList(),
        habitRecords = emptyList(),
        goals = emptyList(),
        goalSteps = emptyList(),
        reviews = emptyList()
    )
}
