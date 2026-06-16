package com.jimu.app.viewmodel

import com.jimu.app.data.local.entity.ReviewEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTodayReviewUiModelTest {

    @Test
    fun fromReviewKeepsAllReviewTextForHomePreview() {
        val review = ReviewEntity(
            reviewDate = "2026-06-16",
            summary = "测试",
            problems = "问题1\n问题2\n问题3",
            tomorrowFocus = "做好1\n做好2\n做好3"
        )

        val uiModel = HomeTodayReviewUiModel.fromReview(
            reviewDate = "2026-06-16",
            review = review
        )

        assertTrue(uiModel.hasReview)
        assertEquals("测试", uiModel.summary)
        assertEquals("问题1\n问题2\n问题3", uiModel.problems)
        assertEquals("做好1\n做好2\n做好3", uiModel.tomorrowFocus)
    }
}
