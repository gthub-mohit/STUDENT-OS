package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studentos.core.database.entity.DailyBriefEntity
import com.studentos.core.database.relation.DailyBriefSummary
import kotlinx.coroutines.flow.Flow

/**
 * DailyBriefDao — Room DAO interface for `daily_briefs` table operations.
 */
@Dao
interface DailyBriefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(brief: DailyBriefEntity): Long

    @Query("UPDATE daily_briefs SET llm_guidance = :guidance, guidance_source = :source, guidance_updated_at = :updatedAt WHERE date = :date")
    suspend fun updateGuidance(date: String, guidance: String, source: String, updatedAt: Long)

    @Query("UPDATE daily_briefs SET score_actual = :scoreActual WHERE date = :date")
    suspend fun updateScoreActual(date: String, scoreActual: Int)

    @Query("SELECT * FROM daily_briefs WHERE date = :date")
    fun getBriefForDate(date: String): Flow<DailyBriefEntity?>

    @Query("SELECT * FROM daily_briefs ORDER BY date DESC")
    fun getAllBriefs(): Flow<List<DailyBriefEntity>>

    @Query("SELECT id, date, score_target, score_actual, guidance_source, generated_at FROM daily_briefs ORDER BY date DESC")
    fun getBriefSummaries(): Flow<List<DailyBriefSummary>>

    @Query("SELECT * FROM daily_briefs WHERE snapshot_hash = :hash LIMIT 1")
    suspend fun getBriefByHash(hash: String): DailyBriefEntity?

    @Query("SELECT * FROM daily_briefs ORDER BY date DESC LIMIT :limit")
    suspend fun getScoreHistory(limit: Int): List<DailyBriefEntity>
}
