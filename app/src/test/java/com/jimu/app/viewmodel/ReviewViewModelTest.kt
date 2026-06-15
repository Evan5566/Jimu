package com.jimu.app.viewmodel

import com.jimu.app.data.local.entity.ReviewEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewViewModelTest {

    @Test
    fun reviewFormStateDisablesSavingWhenSummaryIsBlank() {
        val state = ReviewFormUiState(
            reviewDate = "2026-06-13",
            summary = "   ",
            problems = "迁移回归还要补测",
            tomorrowFocus = "做复盘入口"
        )

        assertFalse(state.canSave)
    }

    @Test
    fun reviewFormStateEnablesSavingWhenSummaryHasText() {
        val state = ReviewFormUiState(
            reviewDate = "2026-06-13",
            summary = "完成复盘闭环",
            problems = "",
            tomorrowFocus = ""
        )

        assertTrue(state.canSave)
    }

    @Test
    fun reviewFormStatePrefillsFromExistingReview() {
        val review = ReviewEntity(
            reviewDate = "2026-06-13",
            summary = "今天把数据层接到界面",
            problems = "还没有设备回归",
            tomorrowFocus = "补手测"
        )

        val state = ReviewFormUiState.fromReview(
            reviewDate = "2026-06-13",
            review = review
        )

        assertEquals("2026-06-13", state.reviewDate)
        assertEquals("今天把数据层接到界面", state.summary)
        assertEquals("还没有设备回归", state.problems)
        assertEquals("补手测", state.tomorrowFocus)
        assertTrue(state.canSave)
    }
}
