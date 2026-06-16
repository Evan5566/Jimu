package com.jimu.app.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeCompletionPaceUiModelTest {

    @Test
    fun fromUsesCurrentCompletionWordingWithoutTimeBasedCompletionClaims() {
        val card = HomeCompletionPaceUiModel.from(
            completedCount = 5,
            todoCount = 2,
            goalFocus = HomeGoalFocusUiModel(
                goalCount = 1,
                totalSteps = 5,
                completedSteps = 2,
                progress = 40
            ),
            todayReview = HomeTodayReviewUiModel(hasReview = true)
        )

        assertEquals("完成节奏", card.title)
        assertEquals("5", card.value)
        assertEquals("当前已完成", card.subtitle)
        assertEquals(
            listOf("当前待处理 2 项", "目标推进 40%", "今日复盘已记录"),
            card.detailLines
        )

        val allText = (listOf(card.title, card.value, card.subtitle) + card.detailLines)
            .joinToString(" ")
        assertFalse(allText.contains("今天完成"))
        assertFalse(allText.contains("今日完成"))
        assertFalse(allText.contains("本周完成"))
    }
}
