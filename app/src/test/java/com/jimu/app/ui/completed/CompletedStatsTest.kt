package com.jimu.app.ui.completed

import com.jimu.app.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletedStatsTest {

    @Test
    fun buildCompletedStatsKeepsOnlyCurrentTotalAndLatestRecordText() {
        val tasks = listOf(
            TaskEntity(id = 1L, title = "较早完成", isCompleted = true, updatedAt = 1_700_000_000_000L),
            TaskEntity(id = 2L, title = "最近完成", isCompleted = true, updatedAt = 1_700_010_000_000L)
        )

        val stats = buildCompletedStats(tasks)

        assertEquals(2, stats.totalCount)
        assertTrue(stats.latestText.startsWith("最近一次记录："))
    }
}
