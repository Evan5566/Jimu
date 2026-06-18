package com.jimu.app.data.backup

import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

    @Test
    fun validPayloadReturnsValidatedWrapper() {
        val result = BackupValidator.validate(sampleBackupPayload())

        assertTrue(result is BackupValidationResult.Valid)
    }

    @Test
    fun duplicateTaskIdIsRejected() {
        val payload = sampleBackupPayload()
        val result = BackupValidator.validate(
            payload.copy(tasks = payload.tasks + payload.tasks.single())
        )

        assertTrue(result is BackupValidationResult.Invalid)
    }

    @Test
    fun orphanHabitRecordIsRejected() {
        val payload = sampleBackupPayload()
        val result = BackupValidator.validate(
            payload.copy(
                habitRecords = payload.habitRecords.map { it.copy(habitId = 999L) }
            )
        )

        assertTrue(result is BackupValidationResult.Invalid)
    }

    @Test
    fun orphanGoalStepIsRejected() {
        val payload = sampleBackupPayload()
        val result = BackupValidator.validate(
            payload.copy(
                goalSteps = payload.goalSteps.map { it.copy(goalId = 999L) }
            )
        )

        assertTrue(result is BackupValidationResult.Invalid)
    }

    @Test
    fun blankRequiredTitleIsRejected() {
        val payload = sampleBackupPayload()
        val result = BackupValidator.validate(
            payload.copy(tasks = payload.tasks.map { it.copy(title = "   ") })
        )

        assertTrue(result is BackupValidationResult.Invalid)
    }

    @Test
    fun invalidDateIsRejected() {
        val payload = sampleBackupPayload()
        val result = BackupValidator.validate(
            payload.copy(
                habitRecords = payload.habitRecords.map { it.copy(recordDate = "2026-02-30") }
            )
        )

        assertTrue(result is BackupValidationResult.Invalid)
    }

    @Test
    fun foreignPackageIsRejected() {
        val payload = sampleBackupPayload()
        val result = BackupValidator.validate(
            payload.copy(meta = payload.meta.copy(appPackage = "example.other"))
        )

        assertTrue(result is BackupValidationResult.Invalid)
    }
}
