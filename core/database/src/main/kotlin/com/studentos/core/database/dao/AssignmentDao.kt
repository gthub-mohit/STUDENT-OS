package com.studentos.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studentos.core.database.entity.AssignmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * AssignmentDao — Room DAO interface for `assignments` table operations.
 */
@Dao
interface AssignmentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(assignment: AssignmentEntity): Long

    @Update
    suspend fun update(assignment: AssignmentEntity)

    @Query("UPDATE assignments SET status = :status, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long)

    @Query("UPDATE assignments SET deadline = :deadline, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateDeadline(id: Long, deadline: Long, updatedAt: Long)

    @Query("UPDATE assignments SET reminder_lead_ms = :reminderLeadMs, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateReminderLeadMs(id: Long, reminderLeadMs: Long?, updatedAt: Long)

    @Query("DELETE FROM assignments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM assignments WHERE status = :status ORDER BY deadline ASC")
    fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE deadline >= :startEpoch AND deadline <= :endEpoch ORDER BY deadline ASC")
    fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE deadline >= :startEpoch AND deadline <= :endEpoch ORDER BY deadline ASC")
    fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE deadline < :nowEpoch AND status NOT IN ('SUBMITTED', 'COMPLETED') ORDER BY deadline ASC")
    fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE id = :id")
    fun getAssignmentById(id: Long): Flow<AssignmentEntity?>

    @Query("SELECT * FROM assignments WHERE deadline <= :withinEpoch AND status NOT IN ('SUBMITTED', 'COMPLETED') ORDER BY deadline ASC")
    suspend fun getUrgentAssignments(withinEpoch: Long): List<AssignmentEntity>
}
