package com.jimu.app.reminder

import com.jimu.app.data.local.entity.TaskEntity

data class TaskReminderPlan(
    val taskId: Long,
    val requestCode: Int,
    val notificationId: Int,
    val title: String,
    val triggerAtMillis: Long
) {
    companion object {
        fun fromTask(
            task: TaskEntity,
            nowMillis: Long = System.currentTimeMillis()
        ): TaskReminderPlan? {
            val dueDate = task.dueDate ?: return null
            if (task.isCompleted || dueDate <= nowMillis) return null

            val stableId = TaskReminderIds.fromTaskId(task.id) ?: return null

            return TaskReminderPlan(
                taskId = task.id,
                requestCode = stableId,
                notificationId = stableId,
                title = task.title,
                triggerAtMillis = dueDate
            )
        }
    }
}
