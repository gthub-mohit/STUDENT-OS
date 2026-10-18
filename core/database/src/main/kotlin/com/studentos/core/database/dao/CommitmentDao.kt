package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.CommitmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * CommitmentDao — Room DAO interface for `commitments` table operations.
 */
@Dao
interface CommitmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(commitment: CommitmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commitments: List<CommitmentEntity>): List<Long>

    @Update
    suspend fun update(commitment: CommitmentEntity)

    @Delete
    suspend fun delete(commitment: CommitmentEntity)

    @Query("DELETE FROM commitments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM commitments WHERE id = :id")
    fun observeCommitmentById(id: Long): Flow<CommitmentEntity?>

    @Query("SELECT * FROM commitments WHERE id = :id")
    suspend fun getCommitmentById(id: Long): CommitmentEntity?

    @Query("SELECT * FROM commitments WHERE status = 'PENDING' ORDER BY CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, deadline ASC")
    fun observePendingCommitments(): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments ORDER BY CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, deadline ASC")
    fun observeAllCommitments(): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments WHERE deadline >= :startEpoch AND deadline <= :endEpoch AND status = 'PENDING' ORDER BY deadline ASC")
    fun observeDueBetween(startEpoch: Long, endEpoch: Long): Flow<List<CommitmentEntity>>

    @Query("SELECT * FROM commitments WHERE status = 'PENDING' ORDER BY CASE WHEN deadline IS NULL THEN 1 ELSE 0 END, deadline ASC")
    suspend fun getPendingCommitments(): List<CommitmentEntity>

    @Query("SELECT * FROM commitments WHERE deadline <= :withinEpoch AND status = 'PENDING' ORDER BY deadline ASC")
    suspend fun getUrgentCommitments(withinEpoch: Long): List<CommitmentEntity>

    @Query("UPDATE commitments SET status = :status, completed_at = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, completedAt: Long?)
}
