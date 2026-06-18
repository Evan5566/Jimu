package com.jimu.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonCodecTest {

    @Test
    fun completePayloadRoundTripsWithoutLosingNullableFields() {
        val payload = sampleBackupPayload()

        val json = BackupJsonCodec.encode(payload)
        val result = BackupJsonCodec.decode(json)

        assertTrue(json.contains("\"backupVersion\":1"))
        assertTrue(result is BackupDecodeResult.Success)
        val decoded = (result as BackupDecodeResult.Success).payload
        assertEquals(payload, decoded)
        assertNull(decoded.tasks.single().description)
        assertNull(decoded.tasks.single().dueDate)
        assertNull(decoded.reviews.single().mood)
    }

    @Test
    fun malformedJsonReturnsInvalidFormat() {
        val result = BackupJsonCodec.decode("not json")

        assertTrue(result is BackupDecodeResult.InvalidFormat)
    }

    @Test
    fun missingVersionReturnsInvalidFormat() {
        val result = BackupJsonCodec.decode("{}")

        assertTrue(result is BackupDecodeResult.InvalidFormat)
    }

    @Test
    fun unknownVersionReturnsUnsupportedVersion() {
        val result = BackupJsonCodec.decode("""{"backupVersion":99}""")

        assertEquals(BackupDecodeResult.UnsupportedVersion(99), result)
    }
}

internal fun sampleBackupPayload(): AppBackupPayloadV1 {
    return AppBackupPayloadV1(
        meta = BackupMetaV1(
            exportedAt = 1_800_000_000_000L,
            appPackage = BACKUP_APP_PACKAGE,
            appVersionName = "1.0",
            appVersionCode = 1L
        ),
        tasks = listOf(
            TaskBackupV1(
                id = 1L,
                title = "task",
                description = null,
                dueDate = null,
                isCompleted = false,
                createdAt = 10L,
                updatedAt = 11L
            )
        ),
        habits = listOf(
            HabitBackupV1(
                id = 2L,
                name = "habit",
                description = "desc",
                createdAt = 12L
            )
        ),
        habitRecords = listOf(
            HabitRecordBackupV1(
                id = 3L,
                habitId = 2L,
                recordDate = "2026-06-18",
                createdAt = 13L
            )
        ),
        goals = listOf(
            GoalBackupV1(
                id = 4L,
                title = "goal",
                description = null,
                createdAt = 14L,
                updatedAt = 15L
            )
        ),
        goalSteps = listOf(
            GoalStepBackupV1(
                id = 5L,
                goalId = 4L,
                title = "step",
                isCompleted = true,
                createdAt = 16L,
                updatedAt = 17L
            )
        ),
        reviews = listOf(
            ReviewBackupV1(
                id = 6L,
                reviewDate = "2026-06-18",
                type = "daily",
                summary = "summary",
                problems = "problems",
                tomorrowFocus = "focus",
                mood = null,
                completedTaskSnapshot = 1,
                checkedHabitSnapshot = 1,
                createdAt = 18L,
                updatedAt = 19L
            )
        )
    )
}
