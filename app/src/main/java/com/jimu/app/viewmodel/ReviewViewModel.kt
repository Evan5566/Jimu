package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jimu.app.data.local.entity.ReviewEntity
import com.jimu.app.data.repository.DailyDigestRepository
import com.jimu.app.data.repository.DailyDigestUiModel
import com.jimu.app.data.repository.ReviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReviewFormUiState(
    val reviewDate: String,
    val summary: String = "",
    val problems: String = "",
    val tomorrowFocus: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null
) {
    val canSave: Boolean
        get() = summary.trim().isNotEmpty() && !isLoading && !isSaving

    companion object {
        fun fromReview(
            reviewDate: String,
            review: ReviewEntity?
        ): ReviewFormUiState {
            return ReviewFormUiState(
                reviewDate = reviewDate,
                summary = review?.summary.orEmpty(),
                problems = review?.problems.orEmpty(),
                tomorrowFocus = review?.tomorrowFocus.orEmpty()
            )
        }
    }
}

class ReviewViewModel(
    private val reviewRepository: ReviewRepository,
    dailyDigestRepository: DailyDigestRepository? = null,
    reviewDate: String = LocalDate.now().toString()
) : ViewModel() {

    private val targetReviewDate = reviewDate.ifBlank { LocalDate.now().toString() }

    private val _uiState = MutableStateFlow(
        ReviewFormUiState(
            reviewDate = targetReviewDate,
            isLoading = true
        )
    )
    val uiState: StateFlow<ReviewFormUiState> = _uiState.asStateFlow()

    val summary: StateFlow<String> = _uiState
        .map { state -> state.summary }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val problems: StateFlow<String> = _uiState
        .map { state -> state.problems }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val tomorrowFocus: StateFlow<String> = _uiState
        .map { state -> state.tomorrowFocus }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val dailyDigest: StateFlow<DailyDigestUiModel> = dailyDigestRepository
        ?.observeDailyDigest()
        ?.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            DailyDigestUiModel.empty(targetReviewDate)
        )
        ?: MutableStateFlow(DailyDigestUiModel.empty(targetReviewDate))

    init {
        loadReview()
    }

    fun onSummaryChange(value: String) {
        _uiState.value = _uiState.value.copy(
            summary = value,
            saveError = null
        )
    }

    fun onProblemsChange(value: String) {
        _uiState.value = _uiState.value.copy(
            problems = value,
            saveError = null
        )
    }

    fun onTomorrowFocusChange(value: String) {
        _uiState.value = _uiState.value.copy(
            tomorrowFocus = value,
            saveError = null
        )
    }

    fun saveReview(onSaved: () -> Unit) {
        val current = _uiState.value
        if (!current.canSave) return

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, saveError = null)

            runCatching {
                reviewRepository.saveDailyReview(
                    reviewDate = current.reviewDate,
                    summary = current.summary,
                    problems = current.problems,
                    tomorrowFocus = current.tomorrowFocus,
                    mood = null,
                    completedTaskSnapshot = 0,
                    checkedHabitSnapshot = 0
                )
            }.onSuccess {
                _uiState.value = _uiState.value.copy(isSaving = false)
                onSaved()
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = "保存失败，请重试"
                )
            }
        }
    }

    private fun loadReview() {
        viewModelScope.launch {
            val review = reviewRepository.getReviewByDate(targetReviewDate)
            _uiState.value = ReviewFormUiState.fromReview(
                reviewDate = targetReviewDate,
                review = review
            )
        }
    }
}

class ReviewViewModelFactory(
    private val reviewRepository: ReviewRepository,
    private val dailyDigestRepository: DailyDigestRepository? = null,
    private val reviewDate: String = LocalDate.now().toString()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReviewViewModel(
                reviewRepository = reviewRepository,
                dailyDigestRepository = dailyDigestRepository,
                reviewDate = reviewDate
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
