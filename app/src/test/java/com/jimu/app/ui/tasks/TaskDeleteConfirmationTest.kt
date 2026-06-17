package com.jimu.app.ui.tasks

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskDeleteConfirmationTest {

    @Test
    fun taskDeleteConfirmationUsesTaskTitle() {
        val content = taskDeleteConfirmationContent("整理发布清单")

        assertEquals("确认删除“整理发布清单”吗？", content)
    }

    @Test
    fun taskDeleteConfirmationTitleIsExplicit() {
        assertEquals("删除待办", taskDeleteConfirmationTitle())
    }
}
