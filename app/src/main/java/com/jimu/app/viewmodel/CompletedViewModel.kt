package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jimu.app.data.local.entity.TaskEntity
import com.jimu.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompletedViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val completedTasks = repository.observeCompletedTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
}

class CompletedViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompletedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CompletedViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}