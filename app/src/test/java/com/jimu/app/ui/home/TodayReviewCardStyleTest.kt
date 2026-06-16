package com.jimu.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TodayReviewCardStyleTest {

    @Test
    fun recordedReviewUsesNeutralCardContainer() {
        assertEquals(
            TodayReviewCardContainerStyle.Neutral,
            todayReviewCardContainerStyle(hasReview = true)
        )
    }
}
