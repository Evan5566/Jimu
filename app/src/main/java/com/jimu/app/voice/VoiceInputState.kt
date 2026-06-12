package com.jimu.app.voice

enum class VoiceInputTarget(val label: String) {
    TASK("待办"),
    HABIT("习惯"),
    GOAL("目标")
}

data class HabitDraft(
    val title: String,
    val description: String = ""
)

data class GoalDraft(
    val title: String
)

sealed interface VoiceInputState {
    data object Idle : VoiceInputState

    data object RequestingPermission : VoiceInputState

    data class Recording(
        val partialText: String = ""
    ) : VoiceInputState

    data object Processing : VoiceInputState

    data class TaskReview(
        val transcript: String,
        val taskDrafts: List<TaskDraft>
    ) : VoiceInputState

    data class HabitReview(
        val transcript: String,
        val habitDraft: HabitDraft
    ) : VoiceInputState

    data class GoalReview(
        val transcript: String,
        val goalDraft: GoalDraft
    ) : VoiceInputState

    data class Error(
        val message: String
    ) : VoiceInputState
}
