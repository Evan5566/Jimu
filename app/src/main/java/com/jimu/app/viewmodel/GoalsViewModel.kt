package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jimu.app.data.local.entity.GoalEntity
import com.jimu.app.data.local.entity.GoalStepEntity
import com.jimu.app.data.repository.GoalOverviewUiModel
import com.jimu.app.data.repository.GoalRepository
import com.jimu.app.data.repository.GoalUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(
    private val repository: GoalRepository
) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _newGoalTitle = MutableStateFlow("")
    val newGoalTitle: StateFlow<String> = _newGoalTitle.asStateFlow()

    private val _stepInputs = MutableStateFlow<Map<Long, String>>(emptyMap())
    val stepInputs: StateFlow<Map<Long, String>> = _stepInputs.asStateFlow()

    private val _editingGoal = MutableStateFlow<GoalEntity?>(null)
    val editingGoal: StateFlow<GoalEntity?> = _editingGoal.asStateFlow()

    private val _editingGoalTitle = MutableStateFlow("")
    val editingGoalTitle: StateFlow<String> = _editingGoalTitle.asStateFlow()

    private val _editingStep = MutableStateFlow<GoalStepEntity?>(null)
    val editingStep: StateFlow<GoalStepEntity?> = _editingStep.asStateFlow()

    private val _editingStepTitle = MutableStateFlow("")
    val editingStepTitle: StateFlow<String> = _editingStepTitle.asStateFlow()

    val goals: StateFlow<List<GoalUiModel>> = repository.observeGoalUiModels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val overview: StateFlow<GoalOverviewUiModel> = repository.observeGoalOverview()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GoalOverviewUiModel(
                totalGoals = 0,
                completedGoals = 0,
                totalSteps = 0,
                completedSteps = 0,
                progress = 0
            )
        )

    fun openAddDialog() {
        _showAddDialog.value = true
    }

    fun closeAddDialog() {
        _showAddDialog.value = false
        _newGoalTitle.value = ""
    }

    fun onNewGoalTitleChange(value: String) {
        _newGoalTitle.value = value
    }

    fun addGoal() {
        viewModelScope.launch {
            repository.addGoal(_newGoalTitle.value)
            closeAddDialog()
        }
    }

    fun addGoalByVoice(recognizedText: String) {
        viewModelScope.launch {
            repository.addGoal(recognizedText)
        }
    }

    fun onStepInputChange(goalId: Long, value: String) {
        _stepInputs.value = _stepInputs.value.toMutableMap().apply {
            this[goalId] = value
        }
    }

    fun addGoalStep(goalId: Long) {
        val currentValue = _stepInputs.value[goalId].orEmpty()
        viewModelScope.launch {
            repository.addGoalStep(goalId, currentValue)
            _stepInputs.value = _stepInputs.value.toMutableMap().apply {
                this[goalId] = ""
            }
        }
    }

    fun openEditGoalDialog(goal: GoalEntity) {
        _editingGoal.value = goal
        _editingGoalTitle.value = goal.title
    }

    fun closeEditGoalDialog() {
        _editingGoal.value = null
        _editingGoalTitle.value = ""
    }

    fun onEditingGoalTitleChange(value: String) {
        _editingGoalTitle.value = value
    }

    fun saveGoalEdit() {
        val goal = _editingGoal.value ?: return
        viewModelScope.launch {
            repository.updateGoalTitle(goal, _editingGoalTitle.value)
            closeEditGoalDialog()
        }
    }

    fun openEditStepDialog(step: GoalStepEntity) {
        _editingStep.value = step
        _editingStepTitle.value = step.title
    }

    fun closeEditStepDialog() {
        _editingStep.value = null
        _editingStepTitle.value = ""
    }

    fun onEditingStepTitleChange(value: String) {
        _editingStepTitle.value = value
    }

    fun saveStepEdit() {
        val step = _editingStep.value ?: return
        viewModelScope.launch {
            repository.updateGoalStepTitle(step, _editingStepTitle.value)
            closeEditStepDialog()
        }
    }

    fun toggleGoalStep(step: GoalStepEntity) {
        viewModelScope.launch {
            repository.toggleGoalStep(step)
        }
    }

    fun restoreGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.restoreGoal(goal.id)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
            _stepInputs.value = _stepInputs.value.toMutableMap().apply {
                remove(goal.id)
            }
        }
    }

    fun deleteGoalStep(step: GoalStepEntity) {
        viewModelScope.launch {
            repository.deleteGoalStep(step)
        }
    }
}

class GoalsViewModelFactory(
    private val repository: GoalRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoalsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GoalsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}