package com.jimu.app.data.backup

import com.jimu.app.data.local.dao.GoalDao
import com.jimu.app.data.local.dao.HabitDao
import com.jimu.app.data.local.dao.ReviewDao
import com.jimu.app.data.local.dao.TaskDao
import com.jimu.app.data.local.entity.GoalEntity
import com.jimu.app.data.local.entity.GoalStepEntity
import com.jimu.app.data.local.entity.HabitEntity
import com.jimu.app.data.local.entity.HabitRecordEntity
import com.jimu.app.data.local.entity.ReviewEntity
import com.jimu.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {

    @Test
    fun emptyDatabaseExportsOneTransactionalSnapshot() = runBlocking {
        val fixture = BackupFixture()

        val json = fixture.repository.exportCurrentBackupJson(sampleMeta())
        val decoded = BackupJsonCodec.decode(json) as BackupDecodeResult.Success

        assertEquals(1, fixture.transactionRunner.callCount)
        assertTrue(decoded.payload.tasks.isEmpty())
        assertTrue(decoded.payload.habits.isEmpty())
        assertTrue(decoded.payload.habitRecords.isEmpty())
        assertTrue(decoded.payload.goals.isEmpty())
        assertTrue(decoded.payload.goalSteps.isEmpty())
        assertTrue(decoded.payload.reviews.isEmpty())
    }

    @Test
    fun nonEmptyDatabaseExportsAllFields() = runBlocking {
        val fixture = BackupFixture().apply { seedOldData() }

        val json = fixture.repository.exportCurrentBackupJson(sampleMeta())
        val decoded = BackupJsonCodec.decode(json) as BackupDecodeResult.Success

        assertEquals("old task", decoded.payload.tasks.single().title)
        assertEquals("old habit", decoded.payload.habits.single().name)
        assertEquals("2026-06-17", decoded.payload.habitRecords.single().recordDate)
        assertEquals("old goal", decoded.payload.goals.single().title)
        assertEquals("old step", decoded.payload.goalSteps.single().title)
        assertEquals("old review", decoded.payload.reviews.single().summary)
    }

    @Test
    fun decodeAndValidateReturnsPreviewAndValidatedPayload() {
        val fixture = BackupFixture()

        val result = fixture.repository.decodeAndValidate(
            BackupJsonCodec.encode(sampleBackupPayload())
        )

        assertTrue(result is BackupImportPreviewResult.ReadyToConfirm)
        val ready = result as BackupImportPreviewResult.ReadyToConfirm
        assertEquals(1, ready.preview.tasks)
        assertEquals(1, ready.preview.habits)
        assertEquals(1, ready.preview.habitRecords)
        assertEquals(1, ready.preview.goals)
        assertEquals(1, ready.preview.goalSteps)
        assertEquals(1, ready.preview.reviews)
        assertEquals(sampleBackupPayload(), ready.validated.payload)
    }

    @Test
    fun invalidJsonDoesNotEnterTransaction() {
        val fixture = BackupFixture()

        val result = fixture.repository.decodeAndValidate("not json")

        assertTrue(result is BackupImportPreviewResult.InvalidFormat)
        assertEquals(0, fixture.transactionRunner.callCount)
    }

    @Test
    fun restoreReplacesAllDataAndReturnsOldTaskIds() = runBlocking {
        val fixture = BackupFixture().apply { seedOldData() }
        val ready = fixture.repository.decodeAndValidate(
            BackupJsonCodec.encode(sampleBackupPayload())
        ) as BackupImportPreviewResult.ReadyToConfirm

        val result = fixture.repository.restoreValidatedPayload(ready.validated)

        assertTrue(result is RestoreDatabaseResult.Success)
        val success = result as RestoreDatabaseResult.Success
        assertEquals(listOf(101L), success.oldTaskIds)
        assertEquals(listOf(1L), fixture.taskDao.tasks.map { it.id })
        assertEquals(listOf(2L), fixture.habitDao.habits.map { it.id })
        assertEquals(listOf(3L), fixture.habitDao.records.map { it.id })
        assertEquals(listOf(4L), fixture.goalDao.goals.map { it.id })
        assertEquals(listOf(5L), fixture.goalDao.steps.map { it.id })
        assertEquals(listOf(6L), fixture.reviewDao.reviews.map { it.id })
    }

    @Test
    fun restoreFailureRollsBackAllTables() = runBlocking {
        val fixture = BackupFixture().apply {
            seedOldData()
            taskDao.failOnRestoreTitle = "task"
        }
        val ready = fixture.repository.decodeAndValidate(
            BackupJsonCodec.encode(sampleBackupPayload())
        ) as BackupImportPreviewResult.ReadyToConfirm

        val result = fixture.repository.restoreValidatedPayload(ready.validated)

        assertTrue(result is RestoreDatabaseResult.RestoreFailed)
        assertEquals(listOf(101L), fixture.taskDao.tasks.map { it.id })
        assertEquals(listOf(102L), fixture.habitDao.habits.map { it.id })
        assertEquals(listOf(103L), fixture.habitDao.records.map { it.id })
        assertEquals(listOf(104L), fixture.goalDao.goals.map { it.id })
        assertEquals(listOf(105L), fixture.goalDao.steps.map { it.id })
        assertEquals(listOf(106L), fixture.reviewDao.reviews.map { it.id })
    }
}

private fun sampleMeta() = BackupMetaV1(
    exportedAt = 1_800_000_000_000L,
    appPackage = BACKUP_APP_PACKAGE,
    appVersionName = "1.0",
    appVersionCode = 1L
)

private class BackupFixture {
    val taskDao = BackupFakeTaskDao()
    val habitDao = BackupFakeHabitDao()
    val goalDao = BackupFakeGoalDao()
    val reviewDao = BackupFakeReviewDao()
    val transactionRunner = SnapshottingFakeTransactionRunner(
        snapshot = {
            BackupSnapshot(
                tasks = taskDao.tasks.toList(),
                habits = habitDao.habits.toList(),
                records = habitDao.records.toList(),
                goals = goalDao.goals.toList(),
                steps = goalDao.steps.toList(),
                reviews = reviewDao.reviews.toList()
            )
        },
        restore = { snapshot ->
            taskDao.tasks.replaceWith(snapshot.tasks)
            habitDao.habits.replaceWith(snapshot.habits)
            habitDao.records.replaceWith(snapshot.records)
            goalDao.goals.replaceWith(snapshot.goals)
            goalDao.steps.replaceWith(snapshot.steps)
            reviewDao.reviews.replaceWith(snapshot.reviews)
        }
    )
    val repository = BackupRepository(
        transactionRunner = transactionRunner,
        taskDao = taskDao,
        habitDao = habitDao,
        goalDao = goalDao,
        reviewDao = reviewDao
    )

    fun seedOldData() {
        taskDao.tasks += TaskEntity(id = 101L, title = "old task")
        habitDao.habits += HabitEntity(id = 102L, name = "old habit")
        habitDao.records += HabitRecordEntity(
            id = 103L,
            habitId = 102L,
            recordDate = "2026-06-17"
        )
        goalDao.goals += GoalEntity(id = 104L, title = "old goal")
        goalDao.steps += GoalStepEntity(id = 105L, goalId = 104L, title = "old step")
        reviewDao.reviews += ReviewEntity(
            id = 106L,
            reviewDate = "2026-06-17",
            summary = "old review",
            problems = "",
            tomorrowFocus = ""
        )
    }
}

private data class BackupSnapshot(
    val tasks: List<TaskEntity>,
    val habits: List<HabitEntity>,
    val records: List<HabitRecordEntity>,
    val goals: List<GoalEntity>,
    val steps: List<GoalStepEntity>,
    val reviews: List<ReviewEntity>
)

private class SnapshottingFakeTransactionRunner(
    private val snapshot: () -> BackupSnapshot,
    private val restore: (BackupSnapshot) -> Unit
) : BackupTransactionRunner {
    var callCount: Int = 0

    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        callCount += 1
        val before = snapshot()
        return try {
            block()
        } catch (error: Throwable) {
            restore(before)
            throw error
        }
    }
}

private fun <T> MutableList<T>.replaceWith(items: List<T>) {
    clear()
    addAll(items)
}

private class BackupFakeTaskDao : TaskDao {
    val tasks = mutableListOf<TaskEntity>()
    var failOnRestoreTitle: String? = null
    private val flow = MutableStateFlow<List<TaskEntity>>(emptyList())

    override fun observeAllTasks(): Flow<List<TaskEntity>> = flow
    override fun observeCompletedTasks(): Flow<List<TaskEntity>> = flow
    override suspend fun getFutureReminderTasks(nowMillis: Long): List<TaskEntity> =
        tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate!! > nowMillis }
    override suspend fun getAllTasksForBackup(): List<TaskEntity> = tasks.sortedBy { it.id }
    override suspend fun insertTask(task: TaskEntity): Long {
        val id = if (task.id == 0L) (tasks.maxOfOrNull { it.id } ?: 0L) + 1L else task.id
        tasks += task.copy(id = id)
        return id
    }
    override suspend fun insertTasksForRestoreAbort(tasks: List<TaskEntity>) {
        if (tasks.any { it.title == failOnRestoreTitle }) error("forced insert failure")
        check(this.tasks.map { it.id }.intersect(tasks.map { it.id }.toSet()).isEmpty())
        this.tasks += tasks
    }
    override suspend fun updateTask(task: TaskEntity) = Unit
    override suspend fun deleteTask(task: TaskEntity) {
        tasks.removeAll { it.id == task.id }
    }
    override suspend fun deleteAllTasksForRestore() {
        tasks.clear()
    }
}

private class BackupFakeHabitDao : HabitDao {
    val habits = mutableListOf<HabitEntity>()
    val records = mutableListOf<HabitRecordEntity>()
    private val habitsFlow = MutableStateFlow<List<HabitEntity>>(emptyList())
    private val recordsFlow = MutableStateFlow<List<HabitRecordEntity>>(emptyList())

    override fun observeAllHabits(): Flow<List<HabitEntity>> = habitsFlow
    override fun observeAllHabitRecords(): Flow<List<HabitRecordEntity>> = recordsFlow
    override suspend fun getAllHabitsForBackup(): List<HabitEntity> = habits.sortedBy { it.id }
    override suspend fun getAllHabitRecordsForBackup(): List<HabitRecordEntity> =
        records.sortedBy { it.id }
    override suspend fun insertHabit(habit: HabitEntity) {
        habits += habit
    }
    override suspend fun insertHabitsForRestoreAbort(habits: List<HabitEntity>) {
        this.habits += habits
    }
    override suspend fun updateHabit(habit: HabitEntity) = Unit
    override suspend fun insertHabitRecord(record: HabitRecordEntity) {
        records += record
    }
    override suspend fun insertHabitRecordsForRestoreAbort(records: List<HabitRecordEntity>) {
        this.records += records
    }
    override suspend fun deleteHabit(habit: HabitEntity) {
        habits.removeAll { it.id == habit.id }
    }
    override suspend fun deleteHabitRecordsByHabitId(habitId: Long) {
        records.removeAll { it.habitId == habitId }
    }
    override suspend fun deleteHabitRecordsByDate(habitId: Long, recordDate: String) {
        records.removeAll { it.habitId == habitId && it.recordDate == recordDate }
    }
    override suspend fun countHabitRecordByDate(habitId: Long, recordDate: String): Int =
        records.count { it.habitId == habitId && it.recordDate == recordDate }
    override suspend fun deleteAllHabitRecordsForRestore() {
        records.clear()
    }
    override suspend fun deleteAllHabitsForRestore() {
        habits.clear()
    }
}

private class BackupFakeGoalDao : GoalDao {
    val goals = mutableListOf<GoalEntity>()
    val steps = mutableListOf<GoalStepEntity>()
    private val goalsFlow = MutableStateFlow<List<GoalEntity>>(emptyList())
    private val stepsFlow = MutableStateFlow<List<GoalStepEntity>>(emptyList())

    override fun observeAllGoals(): Flow<List<GoalEntity>> = goalsFlow
    override fun observeAllGoalSteps(): Flow<List<GoalStepEntity>> = stepsFlow
    override suspend fun getAllGoalsForBackup(): List<GoalEntity> = goals.sortedBy { it.id }
    override suspend fun getAllGoalStepsForBackup(): List<GoalStepEntity> = steps.sortedBy { it.id }
    override suspend fun insertGoal(goal: GoalEntity): Long {
        goals += goal
        return goal.id
    }
    override suspend fun insertGoalsForRestoreAbort(goals: List<GoalEntity>) {
        this.goals += goals
    }
    override suspend fun insertGoalStep(step: GoalStepEntity) {
        steps += step
    }
    override suspend fun insertGoalStepsForRestoreAbort(steps: List<GoalStepEntity>) {
        this.steps += steps
    }
    override suspend fun updateGoal(goal: GoalEntity) = Unit
    override suspend fun updateGoalStep(step: GoalStepEntity) = Unit
    override suspend fun deleteGoal(goal: GoalEntity) {
        goals.removeAll { it.id == goal.id }
    }
    override suspend fun deleteGoalStep(step: GoalStepEntity) {
        steps.removeAll { it.id == step.id }
    }
    override suspend fun deleteGoalStepsByGoalId(goalId: Long) {
        steps.removeAll { it.goalId == goalId }
    }
    override suspend fun resetGoalStepsCompletion(goalId: Long, updatedAt: Long) = Unit
    override suspend fun deleteAllGoalStepsForRestore() {
        steps.clear()
    }
    override suspend fun deleteAllGoalsForRestore() {
        goals.clear()
    }
}

private class BackupFakeReviewDao : ReviewDao {
    val reviews = mutableListOf<ReviewEntity>()
    private val flow = MutableStateFlow<List<ReviewEntity>>(emptyList())

    override fun observeAllReviews(): Flow<List<ReviewEntity>> = flow
    override suspend fun getReviewByDate(reviewDate: String): ReviewEntity? =
        reviews.firstOrNull { it.reviewDate == reviewDate }
    override suspend fun getAllReviewsForBackup(): List<ReviewEntity> = reviews.sortedBy { it.id }
    override suspend fun insertReview(review: ReviewEntity): Long {
        reviews += review
        return review.id
    }
    override suspend fun insertReviewsForRestoreAbort(reviews: List<ReviewEntity>) {
        this.reviews += reviews
    }
    override suspend fun updateReview(review: ReviewEntity) = Unit
    override suspend fun deleteReview(review: ReviewEntity) {
        reviews.removeAll { it.id == review.id }
    }
    override suspend fun deleteAllReviewsForRestore() {
        reviews.clear()
    }
}
