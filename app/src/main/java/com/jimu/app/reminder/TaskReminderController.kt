package com.jimu.app.reminder

import com.jimu.app.data.local.entity.TaskEntity

interface TaskReminderController {
    fun cancel(taskId: Long)

    fun schedule(
        task: TaskEntity,
        mayRequestExactAlarmPermission: Boolean = false
    )
}
