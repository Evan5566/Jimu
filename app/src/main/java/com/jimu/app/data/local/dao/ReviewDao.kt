package com.jimu.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jimu.app.data.local.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {

    @Query("SELECT * FROM daily_reviews ORDER BY reviewDate DESC, updatedAt DESC")
    fun observeAllReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM daily_reviews WHERE reviewDate = :reviewDate AND type = 'daily' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getReviewByDate(reviewDate: String): ReviewEntity?

    @Query("SELECT * FROM daily_reviews ORDER BY id ASC")
    suspend fun getAllReviewsForBackup(): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReviewsForRestoreAbort(reviews: List<ReviewEntity>)

    @Update
    suspend fun updateReview(review: ReviewEntity)

    @Delete
    suspend fun deleteReview(review: ReviewEntity)

    @Query("DELETE FROM daily_reviews")
    suspend fun deleteAllReviewsForRestore()
}
