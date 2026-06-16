package com.jimu.app.ui.completed

import com.jimu.app.data.local.entity.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CompletedTaskGroupsTest {

    @Test
    fun buildCompletedGroupsSortsByUpdatedAtAndUsesFriendlyDayTitles() {
        val todayLater = localDayOffsetMillis(dayOffset = 0, hour = 20, minute = 30)
        val todayEarlier = localDayOffsetMillis(dayOffset = 0, hour = 8, minute = 0)
        val yesterday = localDayOffsetMillis(dayOffset = -1, hour = 12, minute = 0)
        val older = localDayOffsetMillis(dayOffset = -3, hour = 9, minute = 0)

        val groups = buildCompletedGroups(
            listOf(
                completedTask(id = 1L, title = "昨天事项", updatedAt = yesterday),
                completedTask(id = 2L, title = "今天较早", updatedAt = todayEarlier),
                completedTask(id = 3L, title = "更早事项", updatedAt = older),
                completedTask(id = 4L, title = "今天较晚", updatedAt = todayLater)
            )
        )

        assertEquals(3, groups.size)
        assertEquals("今天", groups[0].title)
        assertEquals(listOf("今天较晚", "今天较早"), groups[0].tasks.map { it.title })
        assertEquals("昨天", groups[1].title)
        assertEquals(listOf("昨天事项"), groups[1].tasks.map { it.title })
        assertEquals(expectedDateTitle(older), groups[2].title)
        assertEquals(listOf("更早事项"), groups[2].tasks.map { it.title })
    }

    private fun completedTask(
        id: Long,
        title: String,
        updatedAt: Long
    ): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            isCompleted = true,
            updatedAt = updatedAt
        )
    }

    private fun localDayOffsetMillis(
        dayOffset: Int,
        hour: Int,
        minute: Int
    ): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun expectedDateTitle(timestamp: Long): String {
        return SimpleDateFormat("M月d日", Locale.CHINA).format(Date(timestamp))
    }
}
