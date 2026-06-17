package com.jimu.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class JimuThemeTest {

    @Test
    fun selectJimuColorSchemeUsesDarkSchemeWhenDarkThemeIsTrue() {
        val lightScheme = selectJimuColorScheme(darkTheme = false)
        val darkScheme = selectJimuColorScheme(darkTheme = true)

        assertNotEquals(lightScheme.background, darkScheme.background)
        assertEquals(DeepNavy, darkScheme.background)
        assertEquals(NightBlue, darkScheme.surface)
    }
}
