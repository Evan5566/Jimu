package com.jimu.app.data.repository

import com.jimu.app.data.local.entity.GoalEntity
import com.jimu.app.data.local.entity.GoalStepEntity
import com.jimu.app.data.local.entity.HabitEntity
import com.jimu.app.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailyDigestBuilderTest {

    private val today = LocalDate.of(2026, 6, 16)
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun buildCreatesFriendlyEmptyStateWhenThereIsNoUsefulData() {
        val digest = DailyDigestBuilder.build(
            tasks = emptyList(),
            habits = emptyList(),
            goals = emptyList(),
            today = today,
            zoneId = zoneId
        )

        assertTrue(digest.isEmpty)
        assertEquals("今日成果草稿", digest.title)
        assertEquals(
            listOf("今天还没有可整理的成果，先完成一个待办或打卡一个习惯。"),
            digest.summaryLines
        )
        assertTrue(digest.note.contains("根据当前数据整理"))
    }

    @Test
    fun buildUsesApproximateTaskWordingInsteadOfClaimingTodayCompletion() {
        val digest = DailyDigestBuilder.build(
            tasks = listOf(
                TaskEntity(id = 1L, title = "整理发布路线", isCompleted = true),
                TaskEntity(id = 2L, title = "补 T9 草稿", isCompleted = true),
                TaskEntity(id = 3L, title = "今晚复盘", dueDate = today.atTime(20, 0).millis()),
                TaskEntity(id = 4L, title = "昨天遗留", dueDate = today.minusDays(1).atTime(20, 0).millis())
            ),
            habits = emptyList(),
            goals = emptyList(),
            today = today,
            zoneId = zoneId
        )

        assertFalse(digest.isEmpty)
        assertEquals("当前已完成 2 项待办：整理发布路线、补 T9 草稿。", digest.taskOverview)
        assertEquals("还有 1 项今日待处理、1 项逾期未完成。", digest.reminderOverview)
        assertTrue(digest.summaryLines.contains(digest.taskOverview))
        assertFalse(digest.summaryLines.any { line -> line.contains("今天完成了") })
    }

    @Test
    fun buildSummarizesTodayHabitCheckInsByRecordDateModel() {
        val digest = DailyDigestBuilder.build(
            tasks = emptyList(),
            habits = listOf(
                HabitUiModel(
                    habit = HabitEntity(id = 1L, name = "早起"),
                    checkedToday = true,
                    totalCount = 5,
                    streakCount = 2,
                    recentDoneCount = 2
                ),
                HabitUiModel(
                    habit = HabitEntity(id = 2L, name = "阅读"),
                    checkedToday = false,
                    totalCount = 3,
                    streakCount = 0,
                    recentDoneCount = 1
                )
            ),
            goals = emptyList(),
            today = today,
            zoneId = zoneId
        )

        assertEquals("今日已打卡 1/2 个习惯：早起。", digest.habitOverview)
        assertTrue(digest.summaryLines.contains(digest.habitOverview))
    }

    @Test
    fun buildDescribesGoalProgressAsCurrentProgress() {
        val digest = DailyDigestBuilder.build(
            tasks = emptyList(),
            habits = emptyList(),
            goals = listOf(
                GoalUiModel(
                    goal = GoalEntity(id = 1L, title = "发布准备"),
                    steps = listOf(
                        GoalStepEntity(id = 1L, goalId = 1L, title = "复盘历史", isCompleted = true),
                        GoalStepEntity(id = 2L, goalId = 1L, title = "复盘草稿", isCompleted = false)
                    ),
                    progress = 50
                )
            ),
            today = today,
            zoneId = zoneId
        )

        assertEquals("当前目标推进 1/2 个步骤，整体进度约 50%。", digest.goalOverview)
        assertTrue(digest.summaryLines.contains(digest.goalOverview))
        assertFalse(digest.goalOverview.contains("今日推进"))
    }

    private fun java.time.LocalDateTime.millis(): Long {
        return atZone(zoneId).toInstant().toEpochMilli()
    }
}
