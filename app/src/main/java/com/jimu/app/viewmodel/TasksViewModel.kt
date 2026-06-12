package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jimu.app.data.local.entity.TaskEntity
import com.jimu.app.data.repository.TaskRepository
import com.jimu.app.voice.MockTaskParseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class TaskDateOption {
    NONE,
    TODAY,
    TOMORROW,
    CUSTOM
}

class TasksViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _newTaskTitle = MutableStateFlow("")
    val newTaskTitle: StateFlow<String> = _newTaskTitle.asStateFlow()

    private val _newTaskDateOption = MutableStateFlow(TaskDateOption.NONE)
    val newTaskDateOption: StateFlow<TaskDateOption> = _newTaskDateOption.asStateFlow()

    private val _newTaskCustomDate = MutableStateFlow<Long?>(null)
    val newTaskCustomDate: StateFlow<Long?> = _newTaskCustomDate.asStateFlow()

    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

    private val _editTitle = MutableStateFlow("")
    val editTitle: StateFlow<String> = _editTitle.asStateFlow()

    private val _editDescription = MutableStateFlow("")
    val editDescription: StateFlow<String> = _editDescription.asStateFlow()

    private val _editDateOption = MutableStateFlow(TaskDateOption.NONE)
    val editDateOption: StateFlow<TaskDateOption> = _editDateOption.asStateFlow()

    private val _editCustomDate = MutableStateFlow<Long?>(null)
    val editCustomDate: StateFlow<Long?> = _editCustomDate.asStateFlow()

    private val _newTaskCustomTime = MutableStateFlow<Int?>(null)
    val newTaskCustomTime: StateFlow<Int?> = _newTaskCustomTime.asStateFlow()

    private val _editCustomTime = MutableStateFlow<Int?>(null)
    val editCustomTime: StateFlow<Int?> = _editCustomTime.asStateFlow()

    val tasks = repository.observeAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun openAddDialog() {
        _showAddDialog.value = true
    }

    fun closeAddDialog() {
        _showAddDialog.value = false
        _newTaskTitle.value = ""
        _newTaskDateOption.value = TaskDateOption.NONE
        _newTaskCustomDate.value = null
        _newTaskCustomTime.value = null
    }

    fun onNewTaskTitleChange(value: String) {
        _newTaskTitle.value = value
    }

    fun onNewTaskDateOptionChange(option: TaskDateOption) {
        _newTaskDateOption.value = option
        if (option == TaskDateOption.NONE) {
            _newTaskCustomDate.value = null
            _newTaskCustomTime.value = null
        }
    }

    fun onNewTaskCustomDateChange(timestamp: Long) {
        _newTaskDateOption.value = TaskDateOption.CUSTOM
        _newTaskCustomDate.value = timestamp
    }

    fun onNewTaskCustomTimeChange(minutesOfDay: Int) {
        _newTaskCustomTime.value = minutesOfDay
    }

    fun onEditCustomTimeChange(minutesOfDay: Int) {
        _editCustomTime.value = minutesOfDay
    }

    private fun dateToStartOfDayMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun dueDateFromOption(
        option: TaskDateOption,
        customDate: Long?,
        customTime: Int?
    ): Long? {
        return when (option) {
            TaskDateOption.NONE -> null
            TaskDateOption.TODAY -> {
                val base = dateToStartOfDayMillis(LocalDate.now())
                combineDateAndTime(base, customTime)
            }
            TaskDateOption.TOMORROW -> {
                val base = dateToStartOfDayMillis(LocalDate.now().plusDays(1))
                combineDateAndTime(base, customTime)
            }
            TaskDateOption.CUSTOM -> {
                customDate?.let { combineDateAndTime(it, customTime) }
            }
        }
    }

    fun addTask() {
        viewModelScope.launch {
            repository.addTask(
                title = _newTaskTitle.value,
                dueDate = dueDateFromOption(
                    option = _newTaskDateOption.value,
                    customDate = _newTaskCustomDate.value,
                    customTime = _newTaskCustomTime.value
                )
            )
            closeAddDialog()
        }
    }

    fun addTaskByVoice(recognizedText: String) {
        viewModelScope.launch {
            val parser = MockTaskParseRepository()
            val drafts = parser.parseTasks(recognizedText)

            if (drafts.isEmpty()) {
                repository.addTask(recognizedText)
                return@launch
            }

            drafts.forEach { draft ->
                repository.addTask(
                    title = draft.title,
                    dueDate = draft.dueDateMillis
                )
            }
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun rescheduleTaskToToday(task: TaskEntity) {
        viewModelScope.launch {
            repository.rescheduleTask(
                task = task,
                dueDate = dateToStartOfDayMillis(LocalDate.now())
            )
        }
    }

    fun rescheduleTaskToTomorrow(task: TaskEntity) {
        viewModelScope.launch {
            repository.rescheduleTask(
                task = task,
                dueDate = dateToStartOfDayMillis(LocalDate.now().plusDays(1))
            )
        }
    }

    fun rescheduleTaskToCustomDate(task: TaskEntity, dueDate: Long) {
        viewModelScope.launch {
            repository.rescheduleTask(
                task = task,
                dueDate = dueDate
            )
        }
    }

    fun startEditTask(task: TaskEntity) {
        _editingTask.value = task
        _editTitle.value = task.title
        _editDescription.value = task.description.orEmpty()

        val dueDate = task.dueDate
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        if (dueDate != null) {
            _editCustomTime.value = minutesOfDayFromMillis(dueDate)
        } else {
            _editCustomTime.value = null
        }

        when {
            dueDate == null -> {
                _editDateOption.value = TaskDateOption.NONE
                _editCustomDate.value = null
            }
            dueDate.toLocalDate() == today -> {
                _editDateOption.value = TaskDateOption.TODAY
                _editCustomDate.value = null
            }
            dueDate.toLocalDate() == tomorrow -> {
                _editDateOption.value = TaskDateOption.TOMORROW
                _editCustomDate.value = null
            }
            else -> {
                _editDateOption.value = TaskDateOption.CUSTOM
                _editCustomDate.value = dueDate
            }
        }
    }

    fun onEditTitleChange(value: String) {
        _editTitle.value = value
    }

    fun onEditDescriptionChange(value: String) {
        _editDescription.value = value
    }

    fun onEditDateOptionChange(option: TaskDateOption) {
        _editDateOption.value = option
        if (option == TaskDateOption.NONE) {
            _editCustomDate.value = null
            _editCustomTime.value = null
        }
    }

    fun onEditCustomDateChange(timestamp: Long) {
        _editDateOption.value = TaskDateOption.CUSTOM
        _editCustomDate.value = timestamp
    }

    fun cancelEdit() {
        _editingTask.value = null
        _editTitle.value = ""
        _editDescription.value = ""
        _editDateOption.value = TaskDateOption.NONE
        _editCustomDate.value = null
        _editCustomTime.value = null
    }

    fun saveEdit() {
        val task = _editingTask.value ?: return

        viewModelScope.launch {
            repository.updateTask(
                task = task,
                title = _editTitle.value,
                description = _editDescription.value,
                dueDate = dueDateFromOption(
                    option = _editDateOption.value,
                    customDate = _editCustomDate.value,
                    customTime = _editCustomTime.value
                )
            )
            cancelEdit()
        }
    }
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun minutesOfDayFromMillis(timestamp: Long): Int {
    val time = Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    return time.hour * 60 + time.minute
}

private fun combineDateAndTime(dateMillis: Long, minutesOfDay: Int?): Long {
    if (minutesOfDay == null) return dateMillis

    val zoneId = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(dateMillis)
        .atZone(zoneId)
        .toLocalDate()

    val hour = minutesOfDay / 60
    val minute = minutesOfDay % 60

    return date.atTime(hour, minute)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}

class TasksViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TasksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TasksViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}