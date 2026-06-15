package com.jimu.app.reminder

import com.jimu.app.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TaskReminderPlanTest {

    @Test
    fun fromTaskCreatesPlanForActiveFutureTaskWithDueDate() {
        val task = TaskEntity(
            id = 7L,
            title = "带时间的待办",
            dueDate = 1_800_000_060_000L,
            isCompleted = false
        )

        val plan = TaskReminderPlan.fromTask(
            task = task,
            nowMillis = 1_800_000_000_000L
        )

        assertNotNull(plan)
        assertEquals(7, plan!!.requestCode)
        assertEquals(7, plan.notificationId)
        assertEquals(1_800_000_060_000L, plan.triggerAtMillis)
        assertEquals("带时间的待办", plan.title)
    }

    @Test
    fun fromTaskReturnsNullForTaskWithoutDueDate() {
        val task = TaskEntity(
            id = 8L,
            title = "无时间待办",
            dueDate = null,
            isCompleted = false
        )

        val plan = TaskReminderPlan.fromTask(
            task = task,
            nowMillis = 1_800_000_000_000L
        )

        assertNull(plan)
    }

    @Test
    fun fromTaskReturnsNullForCompletedTask() {
        val task = TaskEntity(
            id = 9L,
            title = "已完成待办",
            dueDate = 1_800_000_060_000L,
            isCompleted = true
        )

        val plan = TaskReminderPlan.fromTask(
            task = task,
            nowMillis = 1_800_000_000_000L
        )

        assertNull(plan)
    }

    @Test
    fun fromTaskReturnsNullForExpiredDueDate() {
        val task = TaskEntity(
            id = 10L,
            title = "过期待办",
            dueDate = 1_799_999_999_999L,
            isCompleted = false
        )

        val plan = TaskReminderPlan.fromTask(
            task = task,
            nowMillis = 1_800_000_000_000L
        )

        assertNull(plan)
    }
}
