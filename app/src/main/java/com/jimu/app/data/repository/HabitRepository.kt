package com.jimu.app.data.repository

import com.jimu.app.data.local.dao.HabitDao
import com.jimu.app.data.local.entity.HabitEntity
import com.jimu.app.data.local.entity.HabitRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

data class HabitUiModel(
    val habit: HabitEntity,
    val checkedToday: Boolean,
    val totalCount: Int,
    val streakCount: Int,
    val recentDoneCount: Int
)

class HabitRepository(
    private val habitDao: HabitDao
) {
    fun observeHabitUiModels(): Flow<List<HabitUiModel>> {
        return combine(
            habitDao.observeAllHabits(),
            habitDao.observeAllHabitRecords()
        ) { habits, records ->
            val today = LocalDate.now()

            habits.map { habit ->
                val habitDates = records
                    .asSequence()
                    .filter { it.habitId == habit.id }
                    .mapNotNull { record ->
                        runCatching { LocalDate.parse(record.recordDate) }.getOrNull()
                    }
                    .toSet()

                val checkedToday = habitDates.contains(today)
                val totalCount = habitDates.size
                val streakCount = calculateStreak(today, habitDates)
                val recentDoneCount = (0..6).count { offset ->
                    habitDates.contains(today.minusDays(offset.toLong()))
                }

                HabitUiModel(
                    habit = habit,
                    checkedToday = checkedToday,
                    totalCount = totalCount,
                    streakCount = streakCount,
                    recentDoneCount = recentDoneCount
                )
            }
        }
    }

    suspend fun addHabit(
        name: String,
        description: String? = null
    ) {
        val finalName = name.trim()
        val finalDescription = description?.trim()?.takeIf { it.isNotBlank() }

        if (finalName.isBlank()) return

        habitDao.insertHabit(
            HabitEntity(
                name = finalName,
                description = finalDescription
            )
        )
    }

    suspend fun updateHabit(
        habit: HabitEntity,
        name: String,
        description: String?
    ) {
        val finalName = name.trim()
        val finalDescription = description?.trim()?.takeIf { it.isNotBlank() }

        if (finalName.isBlank()) return

        habitDao.updateHabit(
            habit.copy(
                name = finalName,
                description = finalDescription
            )
        )
    }

    suspend fun checkInToday(habit: HabitEntity) {
        val today = LocalDate.now().toString()
        val count = habitDao.countHabitRecordByDate(habit.id, today)
        if (count > 0) return

        habitDao.insertHabitRecord(
            HabitRecordEntity(
                habitId = habit.id,
                recordDate = today
            )
        )
    }

    suspend fun uncheckInToday(habit: HabitEntity) {
        val today = LocalDate.now().toString()
        habitDao.deleteHabitRecordsByDate(habit.id, today)
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        habitDao.deleteHabitRecordsByHabitId(habit.id)
        habitDao.deleteHabit(habit)
    }

    private fun calculateStreak(
        today: LocalDate,
        dates: Set<LocalDate>
    ): Int {
        var streak = 0
        var cursor = today

        while (dates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }

        return streak
    }
}
