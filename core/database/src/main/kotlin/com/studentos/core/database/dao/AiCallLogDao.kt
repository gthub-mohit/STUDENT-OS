package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studentos.core.database.entity.AiCallLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * AiCallLogDao — Room DAO interface for `ai_call_log` table operations.
 */
@Dao
interface AiCallLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: AiCallLogEntity): Long

    @Query("DELETE FROM ai_call_log WHERE created_at < :epochMs")
    suspend fun deleteOlderThan(epochMs: Long)

    @Query("SELECT * FROM ai_call_log ORDER BY created_at DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<AiCallLogEntity>>

    @Query("SELECT COUNT(*) FROM ai_call_log WHERE created_at >= :startOfDayEpoch")
    suspend fun countTodaysCalls(startOfDayEpoch: Long): Int

    @Query("SELECT COALESCE(SUM(token_count), 0) FROM ai_call_log WHERE created_at >= :startOfDayEpoch")
    suspend fun sumTodaysTokens(startOfDayEpoch: Long): Int

    @Query("SELECT AVG(CASE WHEN success = 1 THEN 1.0 ELSE 0.0 END) FROM ai_call_log WHERE created_at >= :startEpoch AND created_at <= :endEpoch")
    suspend fun getSuccessRate(startEpoch: Long, endEpoch: Long): Float?
}
