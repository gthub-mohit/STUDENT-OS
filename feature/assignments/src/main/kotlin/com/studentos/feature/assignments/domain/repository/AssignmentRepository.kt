package com.studentos.feature.assignments.domain.repository

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * AssignmentRepository — Domain repository contract for Assignment Engine operations.
 */
interface AssignmentRepository {
    fun getAssignmentById(id: Long): Flow<AssignmentEntity?>
    fun getAllAssignments(): Flow<List<AssignmentEntity>>
    fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>>
    fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>>
    fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>>
    fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>>
    suspend fun getUrgentAssignments(withinEpoch: Long): AppResult<List<AssignmentEntity>>

    suspend fun createAssignment(assignment: AssignmentEntity): AppResult<Long>
    suspend fun updateStatus(id: Long, newStatus: String): AppResult<Unit>
    suspend fun updateDeadline(id: Long, deadline: Long): AppResult<Unit>
    suspend fun updateReminderLead(id: Long, leadMs: Long?): AppResult<Unit>
    suspend fun deleteAssignment(id: Long): AppResult<Unit>
    suspend fun setAttachment(id: Long, uri: String?): AppResult<Unit>
    suspend fun attachFile(id: Long, sourceUriString: String): AppResult<String>
}
