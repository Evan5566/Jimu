package com.jimu.app.reminder

import com.jimu.app.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskReminderIdsTest {

    @Test
    fun usesTaskIdAsReminderIdWhenItFitsInIntRange() {
        assertEquals(42, TaskReminderIds.fromTaskId(42L))
    }

    @Test
    fun rejectsTaskIdsThatCannotFitWithoutCollision() {
        assertNull(TaskReminderIds.fromTaskId(Int.MAX_VALUE.toLong() + 1L))
    }

    @Test
    fun planReturnsNullForTaskIdsThatCannotFitWithoutCollision() {
        val plan = TaskReminderPlan.fromTask(
            task = TaskEntity(
                id = Int.MAX_VALUE.toLong() + 1L,
                title = "future task",
                dueDate = 1_800_000_060_000L
            ),
            nowMillis = 1_800_000_000_000L
        )

        assertNull(plan)
    }
}
