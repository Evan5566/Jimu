package com.jimu.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jimu.app.data.local.entity.ReviewEntity
import com.jimu.app.data.repository.ReviewRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReviewHistoryUiState(
    val reviews: List<ReviewHistoryItemUiModel> = emptyList()
) {
    val isEmpty: Boolean
        get() = reviews.isEmpty()
}

data class ReviewHistoryItemUiModel(
    val reviewDate: String,
    val summaryPreview: String,
    val tomorrowFocusPreview: String
) {
    companion object {
        fun fromReview(review: ReviewEntity): ReviewHistoryItemUiModel {
            return ReviewHistoryItemUiModel(
                reviewDate = review.reviewDate,
                summaryPreview = firstNonBlankLine(review.summary) ?: "未填写做得好的事",
                tomorrowFocusPreview = firstNonBlankLine(review.tomorrowFocus) ?: "未填写明日重点"
            )
        }

        private fun firstNonBlankLine(value: String): String? {
            return value
                .lineSequence()
                .map { line -> line.trim() }
                .firstOrNull { line -> line.isNotEmpty() }
        }
    }
}

class ReviewHistoryViewModel(
    reviewRepository: ReviewRepository
) : ViewModel() {
    val uiState: StateFlow<ReviewHistoryUiState> = reviewRepository.observeAllReviews()
        .map { reviews ->
            ReviewHistoryUiState(
                reviews = reviews
                    .filter { review -> review.type == "daily" }
                    .map { review -> ReviewHistoryItemUiModel.fromReview(review) }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReviewHistoryUiState()
        )
}

class ReviewHistoryViewModelFactory(
    private val reviewRepository: ReviewRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReviewHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReviewHistoryViewModel(reviewRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
