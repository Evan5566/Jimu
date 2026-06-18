package com.jimu.app.data.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jimu.app.data.local.AppDatabase
import com.jimu.app.data.local.entity.TaskEntity
import com.jimu.app.data.repository.TaskRepository
import java.io.Closeable
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreTransactionInstrumentedTest {

    @Test
    fun insertFailureRollsBackDeletionOfOldData() = withFixture { fixture ->
        runBlocking {
            fixture.database.taskDao().insertTask(
                TaskEntity(id = 7L, title = "old task")
            )
            fixture.database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER force_restore_failure
                BEFORE INSERT ON tasks
                WHEN NEW.title = '__force_restore_failure__'
                BEGIN
                    SELECT RAISE(ABORT, 'forced restore failure');
                END
                """.trimIndent()
            )
            val validated = validPayload(
                instrumentedPayload(
                    taskId = 100L,
                    title = "__force_restore_failure__"
                )
            )

            val result = fixture.repository.restoreValidatedPayload(validated)

            assertTrue(result is RestoreDatabaseResult.RestoreFailed)
            val tasks = fixture.database.taskDao().getAllTasksForBackup()
            assertEquals(listOf(7L), tasks.map { it.id })
            assertEquals("old task", tasks.single().title)
        }
    }

    @Test
    fun explicitRestoredIdAdvancesSqliteAutoincrementSequence() = withFixture { fixture ->
        runBlocking {
            val validated = validPayload(instrumentedPayload(taskId = 100L, title = "restored"))

            val restore = fixture.repository.restoreValidatedPayload(validated)
            val added = TaskRepository(fixture.database.taskDao()).addTask("new task")

            assertTrue(restore is RestoreDatabaseResult.Success)
            assertTrue(added != null)
            assertTrue(added!!.id > 100L)
            assertEquals(
                listOf(100L, added.id),
                fixture.database.taskDao().getAllTasksForBackup().map { it.id }
            )
        }
    }

    private fun validPayload(payload: AppBackupPayloadV1): ValidatedBackupPayload {
        return (BackupValidator.validate(payload) as BackupValidationResult.Valid).validated
    }

    private fun withFixture(block: (RoomFixture) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        val fixture = RoomFixture(
            database = database,
            repository = BackupRepository(
                transactionRunner = RoomBackupTransactionRunner(database),
                taskDao = database.taskDao(),
                habitDao = database.habitDao(),
                goalDao = database.goalDao(),
                reviewDao = database.reviewDao()
            )
        )

        fixture.use { block(it) }
    }
}

private data class RoomFixture(
    val database: AppDatabase,
    val repository: BackupRepository
) : Closeable {
    override fun close() {
        database.close()
    }
}
