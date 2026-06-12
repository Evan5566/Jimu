package com.jimu.app.data.repository

import com.jimu.app.data.local.dao.ReviewDao
import com.jimu.app.data.local.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReviewRepositoryTest {

    @Test
    fun saveDailyReviewInsertsDailyReviewForMissingDate() = runBlocking {
        val dao = FakeReviewDao()
        val repository = ReviewRepository(dao)

        repository.saveDailyReview(
            reviewDate = "2026-06-13",
            summary = "finished the data layer",
            problems = "migration needs care",
            tomorrowFocus = "verify on device",
            mood = 4,
            completedTaskSnapshot = 3,
            checkedHabitSnapshot = 2
        )

        val saved = dao.getReviewByDate("2026-06-13")

        assertNotNull(saved)
        assertEquals("2026-06-13", saved!!.reviewDate)
        assertEquals("daily", saved.type)
        assertEquals("finished the data layer", saved.summary)
        assertEquals("migration needs care", saved.problems)
        assertEquals("verify on device", saved.tomorrowFocus)
        assertEquals(4, saved.mood)
        assertEquals(3, saved.completedTaskSnapshot)
        assertEquals(2, saved.checkedHabitSnapshot)
    }

    @Test
    fun saveDailyReviewUpdatesExistingDateInsteadOfInsertingDuplicate() = runBlocking {
        val dao = FakeReviewDao()
        val repository = ReviewRepository(dao)

        repository.saveDailyReview(
            reviewDate = "2026-06-13",
            summary = "first summary",
            problems = "first problem",
            tomorrowFocus = "first focus",
            mood = null,
            completedTaskSnapshot = 1,
            checkedHabitSnapshot = 1
        )
        val first = dao.getReviewByDate("2026-06-13")!!

        repository.saveDailyReview(
            reviewDate = "2026-06-13",
            summary = "updated summary",
            problems = "updated problem",
            tomorrowFocus = "updated focus",
            mood = 5,
            completedTaskSnapshot = 4,
            checkedHabitSnapshot = 3
        )

        val saved = dao.getReviewByDate("2026-06-13")!!

        assertEquals(1, dao.snapshot().size)
        assertEquals(first.id, saved.id)
        assertEquals(first.createdAt, saved.createdAt)
        assertEquals("updated summary", saved.summary)
        assertEquals("updated problem", saved.problems)
        assertEquals("updated focus", saved.tomorrowFocus)
        assertEquals(5, saved.mood)
        assertEquals(4, saved.completedTaskSnapshot)
        assertEquals(3, saved.checkedHabitSnapshot)
    }
}

private class FakeReviewDao : ReviewDao {
    private val reviews = mutableListOf<ReviewEntity>()
    private val reviewsFlow = MutableStateFlow<List<ReviewEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAllReviews(): Flow<List<ReviewEntity>> = reviewsFlow

    override suspend fun getReviewByDate(reviewDate: String): ReviewEntity? {
        return reviews.firstOrNull { review ->
            review.reviewDate == reviewDate && review.type == "daily"
        }
    }

    override suspend fun insertReview(review: ReviewEntity): Long {
        val id = if (review.id == 0L) nextId++ else review.id
        reviews.add(review.copy(id = id))
        publish()
        return id
    }

    override suspend fun updateReview(review: ReviewEntity) {
        val index = reviews.indexOfFirst { it.id == review.id }
        if (index >= 0) {
            reviews[index] = review
        } else {
            reviews.add(review)
        }
        publish()
    }

    override suspend fun deleteReview(review: ReviewEntity) {
        reviews.removeAll { it.id == review.id }
        publish()
    }

    fun snapshot(): List<ReviewEntity> = reviews.toList()

    private fun publish() {
        reviewsFlow.value = snapshot()
    }
}
