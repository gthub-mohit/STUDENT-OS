package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studentos.core.database.entity.RecommendationCacheEntity

/**
 * RecommendationCacheDao — Room DAO interface for `recommendation_cache` table operations.
 */
@Dao
interface RecommendationCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: RecommendationCacheEntity): Long

    @Query("DELETE FROM recommendation_cache WHERE id NOT IN (SELECT id FROM recommendation_cache ORDER BY created_at DESC LIMIT :keepCount)")
    suspend fun deleteOldestBeyondLimit(keepCount: Int = 7)

    @Query("SELECT * FROM recommendation_cache WHERE snapshot_hash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): RecommendationCacheEntity?

    @Query("SELECT * FROM recommendation_cache ORDER BY created_at DESC")
    suspend fun getAll(): List<RecommendationCacheEntity>
}
