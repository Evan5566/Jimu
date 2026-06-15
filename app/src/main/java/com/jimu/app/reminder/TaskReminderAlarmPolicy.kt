package com.jimu.app.reminder

enum class TaskReminderAlarmMode {
    EXACT,
    INEXACT_FALLBACK
}

data class TaskReminderAlarmDecision(
    val mode: TaskReminderAlarmMode,
    val shouldRequestExactAlarmPermission: Boolean
)

object TaskReminderAlarmPolicy {
    fun decide(
        canScheduleExactAlarms: Boolean,
        mayRequestExactAlarmPermission: Boolean
    ): TaskReminderAlarmDecision {
        if (canScheduleExactAlarms) {
            return TaskReminderAlarmDecision(
                mode = TaskReminderAlarmMode.EXACT,
                shouldRequestExactAlarmPermission = false
            )
        }

        return TaskReminderAlarmDecision(
            mode = TaskReminderAlarmMode.INEXACT_FALLBACK,
            shouldRequestExactAlarmPermission = mayRequestExactAlarmPermission
        )
    }
}
