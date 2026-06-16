package com.jimu.app.ui.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewScreenModeTest {

    @Test
    fun topLevelTabModeHidesBackAndUsesPlainSaveText() {
        assertFalse(reviewShowBackButton(isTopLevelTab = true))
        assertEquals("保存", reviewSaveButtonText(isSaving = false, isTopLevelTab = true))
    }

    @Test
    fun secondaryModeKeepsBackAndSaveAndReturnText() {
        assertTrue(reviewShowBackButton(isTopLevelTab = false))
        assertEquals("保存并返回", reviewSaveButtonText(isSaving = false, isTopLevelTab = false))
    }

    @Test
    fun savingTextDoesNotDependOnNavigationMode() {
        assertEquals("保存中...", reviewSaveButtonText(isSaving = true, isTopLevelTab = true))
        assertEquals("保存中...", reviewSaveButtonText(isSaving = true, isTopLevelTab = false))
    }
}
