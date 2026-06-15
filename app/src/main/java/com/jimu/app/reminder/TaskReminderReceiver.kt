package com.jimu.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(TaskReminderScheduler.EXTRA_TITLE) ?: return
        val notificationId = intent.getIntExtra(
            TaskReminderScheduler.EXTRA_NOTIFICATION_ID,
            0
        )

        TaskReminderNotifier.showTaskDueNotification(
            context = context,
            title = title,
            notificationId = notificationId
        )
    }
}
