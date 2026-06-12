package com.jimu.app.voice

data class TaskDraft(
    val title: String,
    val dueDateMillis: Long? = null
)