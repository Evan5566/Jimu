package com.jimu.app.data.repository

import com.jimu.app.data.local.dao.ReviewDao
import com.jimu.app.data.local.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ReviewRepository(
    private val reviewDao: ReviewDao
) {
    fun observeAllReviews(): Flow<List<ReviewEntity>> = reviewDao.observeAllReviews()

    suspend fun getReviewByDate(reviewDate: String): ReviewEntity? {
        return reviewDao.getReviewByDate(reviewDate.trim())
    }

    suspend fun addReview(
        reviewDate: String,
        summary: String,
        problems: String,
        tomorrowFocus: String,
        mood: Int? = null,
        completedTaskSnapshot: Int = 0,
        checkedHabitSnapshot: Int = 0
    ): Long {
        val finalReviewDate = reviewDate.trim()
        if (finalReviewDate.isBlank()) return 0

        return reviewDao.insertReview(
            ReviewEntity(
                reviewDate = finalReviewDate,
                summary = summary.trim(),
                problems = problems.trim(),
                tomorrowFocus = tomorrowFocus.trim(),
                mood = mood,
                completedTaskSnapshot = completedTaskSnapshot,
                checkedHabitSnapshot = checkedHabitSnapshot
            )
        )
    }

    suspend fun updateReview(
        review: ReviewEntity,
        summary: String,
        problems: String,
        tomorrowFocus: String,
        mood: Int?,
        completedTaskSnapshot: Int,
        checkedHabitSnapshot: Int
    ) {
        reviewDao.updateReview(
            review.copy(
                summary = summary.trim(),
                problems = problems.trim(),
                tomorrowFocus = tomorrowFocus.trim(),
                mood = mood,
                completedTaskSnapshot = completedTaskSnapshot,
                checkedHabitSnapshot = checkedHabitSnapshot,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveTodayReview(
        summary: String,
        problems: String,
        tomorrowFocus: String,
        mood: Int? = null,
        completedTaskSnapshot: Int = 0,
        checkedHabitSnapshot: Int = 0
    ) {
        saveDailyReview(
            reviewDate = LocalDate.now().toString(),
            summary = summary,
            problems = problems,
            tomorrowFocus = tomorrowFocus,
            mood = mood,
            completedTaskSnapshot = completedTaskSnapshot,
            checkedHabitSnapshot = checkedHabitSnapshot
        )
    }

    suspend fun saveDailyReview(
        reviewDate: String,
        summary: String,
        problems: String,
        tomorrowFocus: String,
        mood: Int? = null,
        completedTaskSnapshot: Int = 0,
        checkedHabitSnapshot: Int = 0
    ) {
        val finalReviewDate = reviewDate.trim()
        if (finalReviewDate.isBlank()) return

        val existingReview = reviewDao.getReviewByDate(finalReviewDate)

        if (existingReview == null) {
            addReview(
                reviewDate = finalReviewDate,
                summary = summary,
                problems = problems,
                tomorrowFocus = tomorrowFocus,
                mood = mood,
                completedTaskSnapshot = completedTaskSnapshot,
                checkedHabitSnapshot = checkedHabitSnapshot
            )
        } else {
            updateReview(
                review = existingReview,
                summary = summary,
                problems = problems,
                tomorrowFocus = tomorrowFocus,
                mood = mood,
                completedTaskSnapshot = completedTaskSnapshot,
                checkedHabitSnapshot = checkedHabitSnapshot
            )
        }
    }

    suspend fun deleteReview(review: ReviewEntity) {
        reviewDao.deleteReview(review)
    }
}
