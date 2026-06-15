package com.jimu.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.jimu.app.data.local.entity.TaskEntity

class TaskReminderScheduler(
    private val context: Context
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    fun schedule(
        task: TaskEntity,
        mayRequestExactAlarmPermission: Boolean = false
    ) {
        val plan = TaskReminderPlan.fromTask(task) ?: run {
            cancel(task.id)
            return
        }
        val pendingIntent = createPendingIntent(plan)
        val decision = TaskReminderAlarmPolicy.decide(
            canScheduleExactAlarms = canScheduleExactAlarms(),
            mayRequestExactAlarmPermission = mayRequestExactAlarmPermission
        )

        if (decision.shouldRequestExactAlarmPermission) {
            openExactAlarmSettings()
        }

        if (decision.mode == TaskReminderAlarmMode.EXACT) {
            scheduleExact(plan, pendingIntent)
        } else {
            scheduleInexact(plan, pendingIntent)
        }
    }

    fun cancel(taskId: Long) {
        val requestCode = TaskReminderIds.fromTaskId(taskId) ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            createBaseIntent(taskId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun scheduleExact(
        plan: TaskReminderPlan,
        pendingIntent: PendingIntent
    ) {
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                plan.triggerAtMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            scheduleInexact(plan, pendingIntent)
        }
    }

    private fun scheduleInexact(
        plan: TaskReminderPlan,
        pendingIntent: PendingIntent
    ) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            plan.triggerAtMillis,
            pendingIntent
        )
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Some OEM builds omit the exact-alarm settings screen; fallback alarms still run.
        }
    }

    private fun createPendingIntent(plan: TaskReminderPlan): PendingIntent {
        val intent = createBaseIntent(plan.taskId).apply {
            putExtra(EXTRA_TITLE, plan.title)
            putExtra(EXTRA_NOTIFICATION_ID, plan.notificationId)
        }

        return PendingIntent.getBroadcast(
            context,
            plan.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createBaseIntent(taskId: Long): Intent {
        return Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_TASK_DUE
            data = Uri.parse("jimu://task-reminder/$taskId")
        }
    }

    companion object {
        const val ACTION_TASK_DUE = "com.jimu.app.action.TASK_DUE"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
