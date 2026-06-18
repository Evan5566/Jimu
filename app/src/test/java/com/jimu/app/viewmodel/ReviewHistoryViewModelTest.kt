package com.jimu.app.viewmodel

import com.jimu.app.data.local.dao.ReviewDao
import com.jimu.app.data.local.entity.ReviewEntity
import com.jimu.app.data.repository.ReviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun reviewHistoryItemUsesFirstNonBlankLinesAsPreviews() {
        val review = ReviewEntity(
            reviewDate = "2026-06-14",
            summary = "\n完成复盘历史列表\n继续打磨",
            problems = "",
            tomorrowFocus = "\n补实机测试\n准备发布基础"
        )

        val item = ReviewHistoryItemUiModel.fromReview(review)

        assertEquals("2026-06-14", item.reviewDate)
        assertEquals("完成复盘历史列表", item.summaryPreview)
        assertEquals("补实机测试", item.tomorrowFocusPreview)
    }

    @Test
    fun historyStateKeepsDaoOrderAndMapsReviews() = runTest {
        val dao = FakeReviewDao()
        val repository = ReviewRepository(dao)

        dao.insertReview(
            ReviewEntity(
                id = 1,
                reviewDate = "2026-06-15",
                summary = "T7 收口",
                problems = "",
                tomorrowFocus = "T8 历史列表",
                updatedAt = 200
            )
        )
        dao.insertReview(
            ReviewEntity(
                id = 2,
                reviewDate = "2026-06-14",
                summary = "T6 手测通过",
                problems = "",
                tomorrowFocus = "提醒 spike",
                updatedAt = 100
            )
        )

        val viewModel = ReviewHistoryViewModel(repository)
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val items = viewModel.uiState.value.reviews
        collectJob.cancel()

        assertEquals(2, items.size)
        assertEquals("2026-06-15", items[0].reviewDate)
        assertEquals("T7 收口", items[0].summaryPreview)
        assertEquals("T8 历史列表", items[0].tomorrowFocusPreview)
        assertEquals("2026-06-14", items[1].reviewDate)
    }

    @Test
    fun historyStateOnlyShowsDailyReviews() = runTest {
        val dao = FakeReviewDao()
        val repository = ReviewRepository(dao)

        dao.insertReview(
            ReviewEntity(
                id = 1,
                reviewDate = "2026-06-15",
                type = "daily",
                summary = "每日复盘",
                problems = "",
                tomorrowFocus = "继续"
            )
        )
        dao.insertReview(
            ReviewEntity(
                id = 2,
                reviewDate = "2026-W24",
                type = "weekly",
                summary = "周复盘预留记录",
                problems = "",
                tomorrowFocus = "不应混进每日复盘历史"
            )
        )

        val viewModel = ReviewHistoryViewModel(repository)
        val collectJob = backgroundScope.launch {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val items = viewModel.uiState.value.reviews
        collectJob.cancel()

        assertEquals(1, items.size)
        assertEquals("2026-06-15", items.single().reviewDate)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}

private class FakeReviewDao : ReviewDao {
    private val reviews = mutableListOf<ReviewEntity>()
    private val reviewsFlow = MutableStateFlow<List<ReviewEntity>>(emptyList())

    override fun observeAllReviews(): Flow<List<ReviewEntity>> = reviewsFlow

    override suspend fun getReviewByDate(reviewDate: String): ReviewEntity? {
        return reviews.firstOrNull { review ->
            review.reviewDate == reviewDate && review.type == "daily"
        }
    }

    override suspend fun getAllReviewsForBackup(): List<ReviewEntity> =
        reviews.sortedBy { it.id }

    override suspend fun insertReview(review: ReviewEntity): Long {
        reviews.add(review)
        publish()
        return review.id
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

    override suspend fun insertReviewsForRestoreAbort(reviews: List<ReviewEntity>) {
        this.reviews.addAll(reviews)
        publish()
    }

    override suspend fun deleteAllReviewsForRestore() {
        reviews.clear()
        publish()
    }

    private fun publish() {
        reviewsFlow.value = reviews.toList()
    }
}
