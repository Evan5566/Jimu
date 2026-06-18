package com.jimu.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jimu.app.data.local.entity.HabitEntity
import com.jimu.app.data.local.entity.HabitRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_records")
    fun observeAllHabitRecords(): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habits ORDER BY id ASC")
    suspend fun getAllHabitsForBackup(): List<HabitEntity>

    @Query("SELECT * FROM habit_records ORDER BY id ASC")
    suspend fun getAllHabitRecordsForBackup(): List<HabitRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabitsForRestoreAbort(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitRecord(record: HabitRecordEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabitRecordsForRestoreAbort(records: List<HabitRecordEntity>)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habit_records WHERE habitId = :habitId")
    suspend fun deleteHabitRecordsByHabitId(habitId: Long)

    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND recordDate = :recordDate")
    suspend fun deleteHabitRecordsByDate(habitId: Long, recordDate: String)

    @Query("SELECT COUNT(*) FROM habit_records WHERE habitId = :habitId AND recordDate = :recordDate")
    suspend fun countHabitRecordByDate(habitId: Long, recordDate: String): Int

    @Query("DELETE FROM habit_records")
    suspend fun deleteAllHabitRecordsForRestore()

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabitsForRestore()
}
