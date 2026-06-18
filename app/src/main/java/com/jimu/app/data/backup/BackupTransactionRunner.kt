package com.jimu.app.data.backup

import androidx.room.withTransaction
import com.jimu.app.data.local.AppDatabase

interface BackupTransactionRunner {
    suspend fun <T> runInTransaction(block: suspend () -> T): T
}

class RoomBackupTransactionRunner(
    private val database: AppDatabase
) : BackupTransactionRunner {
    override suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return database.withTransaction(block)
    }
}
