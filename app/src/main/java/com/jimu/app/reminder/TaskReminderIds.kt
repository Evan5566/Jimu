package com.jimu.app.reminder

object TaskReminderIds {
    fun fromTaskId(taskId: Long): Int? {
        if (taskId <= 0L || taskId > Int.MAX_VALUE.toLong()) return null
        return taskId.toInt()
    }
}
