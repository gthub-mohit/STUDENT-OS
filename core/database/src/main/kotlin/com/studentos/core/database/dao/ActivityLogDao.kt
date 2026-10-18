package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * ActivityLogDao — Room DAO interface for `activity_logs` table operations.
 */
@Dao
interface ActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ActivityLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ActivityLogEntity>): List<Long>

    @Update
    suspend fun update(log: ActivityLogEntity)

    @Delete
    suspend fun delete(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM activity_logs WHERE id = :id")
    fun observeLogById(id: Long): Flow<ActivityLogEntity?>

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun observeAllLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE goal_id = :goalId ORDER BY timestamp ASC")
    fun observeLogsForGoal(goalId: Long): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE goal_id = :goalId ORDER BY timestamp ASC")
    suspend fun getLogsForGoal(goalId: Long): List<ActivityLogEntity>

    @Query("SELECT * FROM activity_logs WHERE timestamp >= :startEpoch AND timestamp <= :endEpoch ORDER BY timestamp DESC")
    fun observeLogsBetween(startEpoch: Long, endEpoch: Long): Flow<List<ActivityLogEntity>>

    @Query("SELECT COALESCE(SUM(metric_delta), 0.0) FROM activity_logs WHERE goal_id = :goalId")
    fun observeSumDeltaForGoal(goalId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(metric_delta), 0.0) FROM activity_logs WHERE goal_id = :goalId")
    suspend fun getSumDeltaForGoal(goalId: Long): Double

    @Query("SELECT * FROM activity_logs WHERE dedup_hash = :dedupHash LIMIT 1")
    suspend fun findByDedupHash(dedupHash: String): ActivityLogEntity?
}
