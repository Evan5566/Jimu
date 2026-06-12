package com.jimu.app.voice

interface TaskParseRepository {
    suspend fun parseTasks(text: String): List<TaskDraft>
}