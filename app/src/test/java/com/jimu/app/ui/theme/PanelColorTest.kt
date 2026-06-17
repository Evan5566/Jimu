package com.jimu.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PanelColorTest {

    @Test
    fun panelColorUsesLightPanelInLightTheme() {
        assertEquals(PanelBlue, panelColor(darkTheme = false))
    }

    @Test
    fun panelColorUsesDarkSurfaceInDarkTheme() {
        assertEquals(NightBlue, panelColor(darkTheme = true))
    }

    @Test
    fun panelColorDiffersBetweenThemes() {
        assertNotEquals(panelColor(darkTheme = false), panelColor(darkTheme = true))
    }
}
