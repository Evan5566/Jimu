package com.jimu.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskReminderAlarmPolicyTest {

    @Test
    fun choosesExactAlarmWhenPermissionIsAvailable() {
        val decision = TaskReminderAlarmPolicy.decide(
            canScheduleExactAlarms = true,
            mayRequestExactAlarmPermission = true
        )

        assertEquals(TaskReminderAlarmMode.EXACT, decision.mode)
        assertFalse(decision.shouldRequestExactAlarmPermission)
    }

    @Test
    fun fallsBackAndRequestsPermissionWhenExactAlarmPermissionIsMissing() {
        val decision = TaskReminderAlarmPolicy.decide(
            canScheduleExactAlarms = false,
            mayRequestExactAlarmPermission = true
        )

        assertEquals(TaskReminderAlarmMode.INEXACT_FALLBACK, decision.mode)
        assertTrue(decision.shouldRequestExactAlarmPermission)
    }

    @Test
    fun fallsBackWithoutRequestingPermissionDuringBackgroundRestore() {
        val decision = TaskReminderAlarmPolicy.decide(
            canScheduleExactAlarms = false,
            mayRequestExactAlarmPermission = false
        )

        assertEquals(TaskReminderAlarmMode.INEXACT_FALLBACK, decision.mode)
        assertFalse(decision.shouldRequestExactAlarmPermission)
    }
}
