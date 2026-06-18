package com.jimu.app.viewmodel

import com.jimu.app.data.local.dao.ReviewDao
import com.jimu.app.data.local.entity.ReviewEntity
import com.jimu.app.data.local.entity.TaskEntity
import com.jimu.app.data.repository.DailyDigestRepository
import com.jimu.app.data.repository.ReviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = ReviewMainDispatcherRule()

    @Test
    fun reviewFormStateDisablesSavingWhenSummaryIsBlank() {
        val state = ReviewFormUiState(
            reviewDate = "2026-06-13",
            summary = "   ",
            problems = "迁移回归还要补测",
            tomorrowFocus = "做复盘入口"
        )

        assertFalse(state.canSave)
    }

    @Test
    fun reviewFormStateEnablesSavingWhenSummaryHasText() {
        val state = ReviewFormUiState(
            reviewDate = "2026-06-13",
            summary = "完成复盘闭环",
            problems = "",
            tomorrowFocus = ""
        )

        assertTrue(state.canSave)
    }

    @Test
    fun reviewFormStatePrefillsFromExistingReview() {
        val review = ReviewEntity(
            reviewDate = "2026-06-13",
            summary = "今天把数据层接到界面",
            problems = "还没有设备回归",
            tomorrowFocus = "补手测"
        )

        val state = ReviewFormUiState.fromReview(
            reviewDate = "2026-06-13",
            review = review
        )

        assertEquals("2026-06-13", state.reviewDate)
        assertEquals("今天把数据层接到界面", state.summary)
        assertEquals("还没有设备回归", state.problems)
        assertEquals("补手测", state.tomorrowFocus)
        assertTrue(state.canSave)
    }

    @Test
    fun saveReviewPersistsSelectedDateInsteadOfToday() = runTest {
        val dao = ReviewViewModelFakeReviewDao()
        val repository = ReviewRepository(dao)
        val selectedDate = "2026-06-14"
        val today = LocalDate.now().toString()

        val viewModel = ReviewViewModel(
            reviewRepository = repository,
            reviewDate = selectedDate
        )
        advanceUntilIdle()

        viewModel.onSummaryChange("修改旧复盘")
        viewModel.onProblemsChange("不能写到今天")
        viewModel.onTomorrowFocusChange("继续发布准备")
        var saved = false

        viewModel.saveReview {
            saved = true
        }
        advanceUntilIdle()

        val selectedReview = dao.getReviewByDate(selectedDate)
        val todayReview = dao.getReviewByDate(today)

        assertTrue(saved)
        assertNotNull(selectedReview)
        assertEquals("修改旧复盘", selectedReview!!.summary)
        assertEquals("不能写到今天", selectedReview.problems)
        assertEquals("继续发布准备", selectedReview.tomorrowFocus)
        if (selectedDate != today) {
            assertNull(todayReview)
        }
    }

    @Test
    fun dailyDigestStateLoadsFromDigestRepositoryWithoutChangingSavePayload() = runTest {
        val dao = ReviewViewModelFakeReviewDao()
        val repository = ReviewRepository(dao)
        val tasksFlow = MutableStateFlow(
            listOf(
                TaskEntity(id = 1L, title = "完成 T9 草稿", isCompleted = true)
            )
        )
        val digestRepository = DailyDigestRepository(
            tasks = tasksFlow,
            habits = MutableStateFlow(emptyList()),
            goals = MutableStateFlow(emptyList()),
            todayProvider = { LocalDate.of(2026, 6, 16) }
        )

        val viewModel = ReviewViewModel(
            reviewRepository = repository,
            dailyDigestRepository = digestRepository,
            reviewDate = "2026-06-16"
        )
        advanceUntilIdle()

        assertEquals(
            "当前已完成 1 项待办：完成 T9 草稿。",
            viewModel.dailyDigest.value.taskOverview
        )

        viewModel.onSummaryChange("手填复盘内容")
        viewModel.saveReview {}
        advanceUntilIdle()

        val savedReview = dao.getReviewByDate("2026-06-16")
        assertNotNull(savedReview)
        assertEquals("手填复盘内容", savedReview!!.summary)
        assertEquals(0, savedReview.completedTaskSnapshot)
        assertEquals(0, savedReview.checkedHabitSnapshot)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewMainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}

private class ReviewViewModelFakeReviewDao : ReviewDao {
    private val reviews = mutableListOf<ReviewEntity>()
    private val reviewsFlow = MutableStateFlow<List<ReviewEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAllReviews(): Flow<List<ReviewEntity>> = reviewsFlow

    override suspend fun getReviewByDate(reviewDate: String): ReviewEntity? {
        return reviews.firstOrNull { review ->
            review.reviewDate == reviewDate && review.type == "daily"
        }
    }

    override suspend fun getAllReviewsForBackup(): List<ReviewEntity> =
        reviews.sortedBy { it.id }

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
