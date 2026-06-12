package com.jimu.app.voice

import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class MockTaskParseRepository : TaskParseRepository {

    override suspend fun parseTasks(text: String): List<TaskDraft> {
        delay(200)

        val normalized = text.trim()
        if (normalized.isBlank()) return emptyList()

        val parts = splitSentences(normalized)
        return parts.mapNotNull { sentence ->
            parseSingleSentence(sentence)
        }
    }

    private fun splitSentences(text: String): List<String> {
        return text
            .replace("，然后", "，")
            .replace("然后", "，")
            .replace("顺便", "，")
            .replace("并且", "，")
            .replace("以及", "，")
            .replace(",", "，")
            .split("，")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun parseSingleSentence(raw: String): TaskDraft? {
        val now = LocalDateTime.now()
        val zoneId = ZoneId.systemDefault()

        val parsedDate = parseDate(raw, now.toLocalDate())
        val parsedTime = parseTime(raw)
        val relativeDateTime = parseRelativeDateTime(raw, now)

        val title = cleanTitle(raw)
        if (title.isBlank()) return null

        val dueDateMillis = when {
            relativeDateTime != null -> {
                relativeDateTime.atZone(zoneId).toInstant().toEpochMilli()
            }

            parsedDate != null && parsedTime != null -> {
                LocalDateTime.of(parsedDate, parsedTime)
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }

            parsedDate != null -> {
                LocalDateTime.of(parsedDate, defaultTimeBySentence(raw))
                    .atZone(zoneId)
                    .toInstant()
                    .toEpochMilli()
            }

            else -> null
        }

        return TaskDraft(
            title = title,
            dueDateMillis = dueDateMillis
        )
    }

    private fun parseRelativeDateTime(text: String, now: LocalDateTime): LocalDateTime? {
        val hourLater = Regex("([一二两三四五六七八九十\\d]+)\\s*个?小时后").find(text)
        if (hourLater != null) {
            val hours = chineseOrDigitToInt(hourLater.groupValues[1])
            if (hours != null && hours > 0) {
                return now.plusHours(hours.toLong())
            }
        }

        val dayLater = Regex("([一二两三四五六七八九十\\d]+)\\s*天后").find(text)
        if (dayLater != null) {
            val days = chineseOrDigitToInt(dayLater.groupValues[1])
            if (days != null && days > 0) {
                return now.plusDays(days.toLong()).withHour(defaultTimeBySentence(text).hour)
                    .withMinute(defaultTimeBySentence(text).minute)
            }
        }

        return null
    }

    private fun parseDate(text: String, today: LocalDate): LocalDate? {
        return when {
            text.contains("今天") || text.contains("今日") || text.contains("今晚") || text.contains("今早") || text.contains("今天晚上") -> today
            text.contains("明天") || text.contains("明早") || text.contains("明晚") || text.contains("明天下午") || text.contains("明天上午") -> today.plusDays(1)
            text.contains("后天") -> today.plusDays(2)
            text.contains("大后天") -> today.plusDays(3)
            text.contains("本周一") -> thisWeek(today, DayOfWeek.MONDAY)
            text.contains("本周二") -> thisWeek(today, DayOfWeek.TUESDAY)
            text.contains("本周三") -> thisWeek(today, DayOfWeek.WEDNESDAY)
            text.contains("本周四") -> thisWeek(today, DayOfWeek.THURSDAY)
            text.contains("本周五") -> thisWeek(today, DayOfWeek.FRIDAY)
            text.contains("本周六") -> thisWeek(today, DayOfWeek.SATURDAY)
            text.contains("本周日") || text.contains("本周天") -> thisWeek(today, DayOfWeek.SUNDAY)
            text.contains("下周一") -> nextWeek(today, DayOfWeek.MONDAY)
            text.contains("下周二") -> nextWeek(today, DayOfWeek.TUESDAY)
            text.contains("下周三") -> nextWeek(today, DayOfWeek.WEDNESDAY)
            text.contains("下周四") -> nextWeek(today, DayOfWeek.THURSDAY)
            text.contains("下周五") -> nextWeek(today, DayOfWeek.FRIDAY)
            text.contains("下周六") -> nextWeek(today, DayOfWeek.SATURDAY)
            text.contains("下周日") || text.contains("下周天") -> nextWeek(today, DayOfWeek.SUNDAY)
            text.contains("周一") -> nextOrSame(today, DayOfWeek.MONDAY)
            text.contains("周二") -> nextOrSame(today, DayOfWeek.TUESDAY)
            text.contains("周三") -> nextOrSame(today, DayOfWeek.WEDNESDAY)
            text.contains("周四") -> nextOrSame(today, DayOfWeek.THURSDAY)
            text.contains("周五") -> nextOrSame(today, DayOfWeek.FRIDAY)
            text.contains("周六") -> nextOrSame(today, DayOfWeek.SATURDAY)
            text.contains("周日") || text.contains("周天") -> nextOrSame(today, DayOfWeek.SUNDAY)
            text.contains("周末") -> nextWeekend(today)
            else -> null
        }
    }

    private fun parseTime(text: String): LocalTime? {
        val colonRegex = Regex("(上午|中午|下午|晚上|今晚|早上|早晨|凌晨)?\\s*(\\d{1,2})[:：](\\d{1,2})")
        val colonMatch = colonRegex.find(text)
        if (colonMatch != null) {
            val period = colonMatch.groupValues[1]
            val hourRaw = colonMatch.groupValues[2].toIntOrNull()
            val minuteRaw = colonMatch.groupValues[3].toIntOrNull()
            if (hourRaw != null && minuteRaw != null) {
                val hour = adjustHourByPeriod(hourRaw, period)
                return safeTime(hour, minuteRaw)
            }
        }

        val halfRegex = Regex("(上午|中午|下午|晚上|今晚|早上|早晨|凌晨)?\\s*([一二两三四五六七八九十\\d]{1,3})\\s*点半")
        val halfMatch = halfRegex.find(text)
        if (halfMatch != null) {
            val period = halfMatch.groupValues[1]
            val hourRaw = chineseOrDigitToInt(halfMatch.groupValues[2])
            if (hourRaw != null) {
                val hour = adjustHourByPeriod(hourRaw, period)
                return safeTime(hour, 30)
            }
        }

        val hourRegex = Regex("(上午|中午|下午|晚上|今晚|早上|早晨|凌晨)?\\s*([一二两三四五六七八九十\\d]{1,3})\\s*点")
        val hourMatch = hourRegex.find(text)
        if (hourMatch != null) {
            val period = hourMatch.groupValues[1]
            val hourRaw = chineseOrDigitToInt(hourMatch.groupValues[2])
            if (hourRaw != null) {
                val hour = adjustHourByPeriod(hourRaw, period)
                return safeTime(hour, 0)
            }
        }

        return null
    }

    private fun adjustHourByPeriod(hourRaw: Int, period: String): Int {
        val hour = hourRaw.coerceIn(0, 23)
        return when (period) {
            "下午", "晚上", "今晚" -> if (hour in 1..11) hour + 12 else hour
            "中午" -> if (hour in 1..10) hour + 12 else hour
            "凌晨" -> if (hour == 12) 0 else hour
            else -> hour
        }
    }

    private fun safeTime(hour: Int, minute: Int): LocalTime? {
        return try {
            LocalTime.of(hour, minute.coerceIn(0, 59))
        } catch (_: Exception) {
            null
        }
    }

    private fun defaultTimeBySentence(text: String): LocalTime {
        return when {
            text.contains("今早") || text.contains("明早") || text.contains("早上") || text.contains("早晨") -> LocalTime.of(8, 0)
            text.contains("中午") -> LocalTime.of(12, 0)
            text.contains("下午") -> LocalTime.of(15, 0)
            text.contains("晚上") || text.contains("今晚") || text.contains("明晚") -> LocalTime.of(19, 0)
            text.contains("周末") -> LocalTime.of(10, 0)
            else -> LocalTime.of(9, 0)
        }
    }

    private fun cleanTitle(text: String): String {
        return text
            .replace(
                Regex(
                    "今天|今日|明天|后天|大后天|今晚|今早|明早|明晚|本周一|本周二|本周三|本周四|本周五|本周六|本周日|本周天|下周一|下周二|下周三|下周四|下周五|下周六|下周日|下周天|周一|周二|周三|周四|周五|周六|周日|周天|周末"
                ),
                ""
            )
            .replace(Regex("^(嗯+|啊+|呃+|那个|这个|就是)+"), "")
            .replace(Regex("^(我想|我想要|我准备|我打算|我要|我要去|准备|要)+"), "")
            .replace(Regex("^(然后|顺便|先)+"), "")
            .replace(Regex("上午|中午|下午|晚上|早上|早晨|凌晨"), "")
            .replace(Regex("([一二两三四五六七八九十\\d]+)\\s*个?小时后"), "")
            .replace(Regex("([一二两三四五六七八九十\\d]+)\\s*天后"), "")
            .replace(Regex("\\d{1,2}[:：]\\d{1,2}"), "")
            .replace(Regex("([一二两三四五六七八九十\\d]{1,3})\\s*点半"), "")
            .replace(Regex("([一二两三四五六七八九十\\d]{1,3})\\s*点"), "")
            .replace("提醒我", "")
            .replace("安排", "")
            .replace("一下", "")
            .replace("要", "")
            .replace("记得", "")
            .replace(Regex("\\s+"), "")
            .trim()
    }

    private fun chineseOrDigitToInt(value: String): Int? {
        val clean = value.trim()
        clean.toIntOrNull()?.let { return it }

        return when (clean) {
            "一" -> 1
            "二", "两" -> 2
            "三" -> 3
            "四" -> 4
            "五" -> 5
            "六" -> 6
            "七" -> 7
            "八" -> 8
            "九" -> 9
            "十" -> 10
            "十一" -> 11
            "十二" -> 12
            "十三" -> 13
            "十四" -> 14
            "十五" -> 15
            "十六" -> 16
            "十七" -> 17
            "十八" -> 18
            "十九" -> 19
            "二十" -> 20
            "二十一" -> 21
            "二十二" -> 22
            "二十三" -> 23
            else -> null
        }
    }

    private fun nextOrSame(today: LocalDate, target: DayOfWeek): LocalDate {
        var date = today
        repeat(7) {
            if (date.dayOfWeek == target) return date
            date = date.plusDays(1)
        }
        return date
    }

    private fun thisWeek(today: LocalDate, target: DayOfWeek): LocalDate {
        val start = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        return start.plusDays((target.value - DayOfWeek.MONDAY.value).toLong())
    }

    private fun nextWeek(today: LocalDate, target: DayOfWeek): LocalDate {
        val thisWeekTarget = thisWeek(today, target)
        return if (thisWeekTarget.isAfter(today)) {
            thisWeekTarget.plusWeeks(1)
        } else {
            thisWeekTarget.plusWeeks(1)
        }
    }

    private fun nextWeekend(today: LocalDate): LocalDate {
        val saturday = nextOrSame(today, DayOfWeek.SATURDAY)
        return saturday
    }
}