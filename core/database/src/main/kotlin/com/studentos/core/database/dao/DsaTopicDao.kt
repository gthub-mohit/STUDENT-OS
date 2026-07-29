package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.DsaTopicEntity
import kotlinx.coroutines.flow.Flow

/**
 * DsaTopicDao — Room DAO interface for `dsa_topics` table operations.
 */
@Dao
interface DsaTopicDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(topic: DsaTopicEntity): Long

    @Update
    suspend fun update(topic: DsaTopicEntity)

    @Query("UPDATE dsa_topics SET confidence_level = :confidenceLevel, revision_status = :revisionStatus, notes = :notes, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateMastery(id: Long, confidenceLevel: Int, revisionStatus: String, notes: String?, updatedAt: Long)

    @Query("DELETE FROM dsa_topics WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM dsa_topics WHERE category_id = :categoryId ORDER BY name ASC")
    fun getTopicsByCategory(categoryId: Long): Flow<List<DsaTopicEntity>>

    @Query("SELECT * FROM dsa_topics WHERE revision_status = :status ORDER BY name ASC")
    fun getTopicsByRevisionStatus(status: String): Flow<List<DsaTopicEntity>>

    @Query("SELECT * FROM dsa_topics WHERE revision_status = :revisionStatus AND confidence_level = :confidenceLevel ORDER BY name ASC")
    fun getTopicsFilteredBy(revisionStatus: String, confidenceLevel: Int): Flow<List<DsaTopicEntity>>

    /**
     * Finds the lowest-confidence non-revised topic for daily revision suggestion.
     */
    @Query("SELECT * FROM dsa_topics WHERE revision_status IN ('NOT_STARTED', 'IN_PROGRESS') ORDER BY confidence_level ASC, id ASC LIMIT 1")
    suspend fun getSuggestedTopic(): DsaTopicEntity?

    /**
     * Checks if all topics in the DSA tree are fully mastered (confidence_level = 5 AND revision_status = 'REVISED').
     * Returns true if 0 topics remain unmastered.
     */
    @Query("SELECT (COUNT(*) = 0) FROM dsa_topics WHERE confidence_level < 5 OR revision_status != 'REVISED'")
    suspend fun getAllMastered(): Boolean

    @Query("SELECT COUNT(*) FROM dsa_topics")
    suspend fun getTopicCount(): Int
}
