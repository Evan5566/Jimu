package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jimu.app.data.repository.GoalRepository
import com.jimu.app.data.repository.HabitRepository
import com.jimu.app.data.repository.TaskRepository
import com.jimu.app.voice.GoalDraft
import com.jimu.app.voice.HabitDraft
import com.jimu.app.voice.SpeechToTextRepository
import com.jimu.app.voice.TaskDraft
import com.jimu.app.voice.TaskParseRepository
import com.jimu.app.voice.VoiceInputState
import com.jimu.app.voice.VoiceInputTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceInputViewModel(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val speechToTextRepository: SpeechToTextRepository,
    private val taskParseRepository: TaskParseRepository
) : ViewModel() {

    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    private var currentTarget: VoiceInputTarget = VoiceInputTarget.TASK

    fun startListening(target: VoiceInputTarget) {
        currentTarget = target
        _state.value = VoiceInputState.Recording("")

        speechToTextRepository.startListening(
            onReady = {
                _state.value = VoiceInputState.Recording("")
            },
            onPartialResult = { partial ->
                _state.value = VoiceInputState.Recording(partialText = partial)
            },
            onFinalResult = { finalText ->
                handleFinalTranscript(finalText)
            },
            onError = { message ->
                _state.value = VoiceInputState.Error(message)
            }
        )
    }

    fun stopListening() {
        _state.value = VoiceInputState.Processing
        speechToTextRepository.stopListening()
    }

    fun cancelListening() {
        speechToTextRepository.cancel()
        _state.value = VoiceInputState.Idle
    }

    fun reset() {
        speechToTextRepository.cancel()
        _state.value = VoiceInputState.Idle
    }

    fun setError(message: String) {
        _state.value = VoiceInputState.Error(message)
    }

    fun addAllTasks(
        drafts: List<TaskDraft>,
        onDone: () -> Unit = {}
    ) {
        if (drafts.isEmpty()) return

        viewModelScope.launch {
            drafts.forEach { draft ->
                val title = draft.title.trim()
                if (title.isNotBlank()) {
                    taskRepository.addTask(
                        title = title,
                        dueDate = draft.dueDateMillis
                    )
                }
            }

            _state.value = VoiceInputState.Idle
            onDone()
        }
    }

    fun addHabit(
        title: String,
        description: String,
        onDone: () -> Unit = {}
    ) {
        val finalTitle = title.trim()
        if (finalTitle.isBlank()) return

        viewModelScope.launch {
            habitRepository.addHabit(
                name = finalTitle,
                description = description
            )
            _state.value = VoiceInputState.Idle
            onDone()
        }
    }

    fun addGoal(
        title: String,
        onDone: () -> Unit = {}
    ) {
        val finalTitle = title.trim()
        if (finalTitle.isBlank()) return

        viewModelScope.launch {
            goalRepository.addGoal(finalTitle)
            _state.value = VoiceInputState.Idle
            onDone()
        }
    }

    private fun handleFinalTranscript(transcript: String) {
        viewModelScope.launch {
            val finalText = transcript.trim()

            if (finalText.isBlank()) {
                _state.value = VoiceInputState.Error("没有识别到有效内容")
                return@launch
            }

            _state.value = VoiceInputState.Processing

            try {
                when (currentTarget) {
                    VoiceInputTarget.TASK -> {
                        val drafts = taskParseRepository.parseTasks(finalText)
                        if (drafts.isEmpty()) {
                            _state.value = VoiceInputState.Error("没有解析出可添加的待办")
                        } else {
                            _state.value = VoiceInputState.TaskReview(
                                transcript = finalText,
                                taskDrafts = drafts
                            )
                        }
                    }

                    VoiceInputTarget.HABIT -> {
                        val title = extractHabitTitle(finalText)
                        if (title.isBlank()) {
                            _state.value = VoiceInputState.Error("没有提取到有效的习惯名称")
                        } else {
                            _state.value = VoiceInputState.HabitReview(
                                transcript = finalText,
                                habitDraft = HabitDraft(
                                    title = title,
                                    description = ""
                                )
                            )
                        }
                    }

                    VoiceInputTarget.GOAL -> {
                        val title = extractGoalTitle(finalText)
                        if (title.isBlank()) {
                            _state.value = VoiceInputState.Error("没有提取到有效的目标名称")
                        } else {
                            _state.value = VoiceInputState.GoalReview(
                                transcript = finalText,
                                goalDraft = GoalDraft(
                                    title = title
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = VoiceInputState.Error(
                    e.message ?: "语音解析失败"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechToTextRepository.release()
    }

    private fun extractHabitTitle(raw: String): String {
        val normalized = raw.trim()

        val cleaned = normalized
            .replace(Regex("^(嗯+|啊+|呃+|那个|这个|就是)+"), "")
            .replace(Regex("^(我想|我想要|我准备|我打算|我要|我要开始|我要养成|帮我|请帮我)+"), "")
            .replace(Regex("(帮我)?(新增|添加|创建|加一个)(一条|一个)?习惯"), "")
            .replace(Regex("(开始|养成|建立|保持)(一个)?习惯"), "")
            .replace(Regex("^(以后|之后|从今天开始|从现在开始)+"), "")
            .replace(Regex("^(每天|每日)+"), "")
            .replace(Regex("^(习惯是|习惯叫做)+"), "")
            .replace(Regex("^(我要坚持|我想坚持)+"), "")
            .replace(Regex("[，。,.；;！!？?]+"), "")
            .replace(Regex("\\s+"), "")
            .trim()

        return cleaned.takeIf { it.isNotBlank() } ?: ""
    }

    private fun extractGoalTitle(raw: String): String {
        val normalized = raw.trim()

        val cleaned = normalized
            .replace(Regex("^(嗯+|啊+|呃+|那个|就是)+"), "")
            .replace(Regex("^(我想|我想要|我准备|我打算|我要|帮我|请帮我)+"), "")
            .replace(Regex("(帮我)?(新增|添加|创建|设定|新建|加一个)(一条|一个)?目标"), "")
            .replace(Regex("(设定|建立|创建)(一条|一个)?目标"), "")
            .replace(Regex("^(目标是|我的目标是|我今年的目标是|我最近的目标是)+"), "")
            .replace(Regex("^(以后|之后|从今天开始|从现在开始)+"), "")
            .replace(Regex("[，。,.；;！!？?]+"), "")
            .replace(Regex("\\s+"), "")
            .trim()
            .let(::normalizeGoalPeriodWords)

        return cleaned.takeIf { it.isNotBlank() } ?: ""
    }

    private fun normalizeGoalPeriodWords(text: String): String {
        return text
            .replace(Regex("^这个月"), "本月")
            .replace(Regex("^当月"), "本月")
            .replace(Regex("^这一个月"), "本月")
            .replace(Regex("^这月"), "本月")
            .replace(Regex("^这个星期"), "本周")
            .replace(Regex("^这星期"), "本周")
            .replace(Regex("^这一周"), "本周")
            .replace(Regex("^这周"), "本周")
            .replace(Regex("^当周"), "本周")
            .replace(Regex("^今年"), "本年")
            .replace(Regex("^这一年"), "本年")
            .replace(Regex("^当年"), "本年")
            .replace(Regex("^今天"), "今日")
    }
}

class VoiceInputViewModelFactory(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val speechToTextRepository: SpeechToTextRepository,
    private val taskParseRepository: TaskParseRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceInputViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VoiceInputViewModel(
                taskRepository = taskRepository,
                habitRepository = habitRepository,
                goalRepository = goalRepository,
                speechToTextRepository = speechToTextRepository,
                taskParseRepository = taskParseRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
