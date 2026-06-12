package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jimu.app.data.local.entity.HabitEntity
import com.jimu.app.data.repository.HabitRepository
import com.jimu.app.data.repository.HabitUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HabitTab {
    TODAY,
    ALL
}

enum class HabitDialogMode {
    ADD,
    EDIT
}

data class HabitSummaryUiState(
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val remainingCount: Int = 0,
    val totalCheckIns: Int = 0
)

class HabitsViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val _dialogMode = MutableStateFlow<HabitDialogMode?>(null)
    val dialogMode: StateFlow<HabitDialogMode?> = _dialogMode.asStateFlow()

    private val _editingHabit = MutableStateFlow<HabitEntity?>(null)
    val editingHabit: StateFlow<HabitEntity?> = _editingHabit.asStateFlow()

    private val _draftName = MutableStateFlow("")
    val draftName: StateFlow<String> = _draftName.asStateFlow()

    private val _draftDescription = MutableStateFlow("")
    val draftDescription: StateFlow<String> = _draftDescription.asStateFlow()

    private val _selectedTab = MutableStateFlow(HabitTab.TODAY)
    val selectedTab: StateFlow<HabitTab> = _selectedTab.asStateFlow()

    val habits: StateFlow<List<HabitUiModel>> = repository.observeHabitUiModels()
        .map { list ->
            list.sortedWith(
                compareBy<HabitUiModel> { it.checkedToday }
                    .thenByDescending { it.streakCount }
                    .thenByDescending { it.totalCount }
                    .thenBy { it.habit.name }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val summary: StateFlow<HabitSummaryUiState> = habits
        .map { list ->
            val completed = list.count { it.checkedToday }
            val total = list.size
            HabitSummaryUiState(
                completedCount = completed,
                totalCount = total,
                remainingCount = (total - completed).coerceAtLeast(0),
                totalCheckIns = list.sumOf { it.totalCount }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HabitSummaryUiState()
        )

    val pendingHabits: StateFlow<List<HabitUiModel>> = habits
        .map { list -> list.filterNot { it.checkedToday } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedHabits: StateFlow<List<HabitUiModel>> = habits
        .map { list -> list.filter { it.checkedToday } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allHabits: StateFlow<List<HabitUiModel>> = habits

    val isEmpty: StateFlow<Boolean> = habits
        .map { it.isEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun selectTab(tab: HabitTab) {
        _selectedTab.value = tab
    }

    fun openAddDialog() {
        _dialogMode.value = HabitDialogMode.ADD
        _editingHabit.value = null
        _draftName.value = ""
        _draftDescription.value = ""
    }

    fun openEditDialog(habit: HabitEntity) {
        _dialogMode.value = HabitDialogMode.EDIT
        _editingHabit.value = habit
        _draftName.value = habit.name
        _draftDescription.value = habit.description.orEmpty()
    }

    fun closeDialog() {
        _dialogMode.value = null
        _editingHabit.value = null
        _draftName.value = ""
        _draftDescription.value = ""
    }

    fun onDraftNameChange(value: String) {
        _draftName.value = value
    }

    fun onDraftDescriptionChange(value: String) {
        _draftDescription.value = value
    }

    fun submitDialog() {
        val mode = _dialogMode.value ?: return
        val name = _draftName.value.trim()
        val description = _draftDescription.value

        if (name.isBlank()) return

        viewModelScope.launch {
            when (mode) {
                HabitDialogMode.ADD -> {
                    repository.addHabit(
                        name = name,
                        description = description
                    )
                }

                HabitDialogMode.EDIT -> {
                    val currentHabit = _editingHabit.value ?: return@launch
                    repository.updateHabit(
                        habit = currentHabit,
                        name = name,
                        description = description
                    )
                }
            }
            closeDialog()
        }
    }

    fun addHabitByVoice(recognizedText: String) {
        val name = recognizedText.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            repository.addHabit(name = name)
        }
    }

    fun checkInToday(habit: HabitEntity) {
        viewModelScope.launch {
            repository.checkInToday(habit)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }
}

class HabitsViewModelFactory(
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}