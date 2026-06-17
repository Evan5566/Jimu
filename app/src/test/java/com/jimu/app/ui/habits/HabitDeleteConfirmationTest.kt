package com.jimu.app.ui.habits

import org.junit.Assert.assertEquals
import org.junit.Test

class HabitDeleteConfirmationTest {

    @Test
    fun habitDeleteConfirmationUsesHabitName() {
        val content = habitDeleteConfirmationContent("早睡")

        assertEquals("确认删除“早睡”吗？历史打卡记录也会一并移除。", content)
    }

    @Test
    fun habitDeleteConfirmationTitleIsExplicit() {
        assertEquals("删除习惯", habitDeleteConfirmationTitle())
    }
}
