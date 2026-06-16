package com.jimu.app.ui.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewScreenModeTest {

    @Test
    fun topLevelTabModeShowsHistoryHidesBackAndUsesPlainSaveText() {
        assertTrue(reviewShowHistoryButton(isTopLevelTab = true))
        assertFalse(reviewShowBackButton(isTopLevelTab = true))
        assertEquals("保存", reviewSaveButtonText(isSaving = false))
    }

    @Test
    fun secondaryModeHidesHistoryKeepsBackAndUsesPlainSaveText() {
        assertFalse(reviewShowHistoryButton(isTopLevelTab = false))
        assertTrue(reviewShowBackButton(isTopLevelTab = false))
        assertEquals("保存", reviewSaveButtonText(isSaving = false))
    }

    @Test
    fun savingTextDoesNotDependOnNavigationMode() {
        assertEquals("保存中...", reviewSaveButtonText(isSaving = true))
    }
}
