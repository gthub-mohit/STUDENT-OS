package com.studentos.feature.attendance.domain.repository

import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.core.events.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * ClassEventRepository — Domain repository contract for class events and status updates.
 */
interface ClassEventRepository {
    fun getEventsForSubject(subjectId: Long): Flow<List<ClassEventEntity>>
    fun getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>>
    fun getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>>
    fun getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>>
    suspend fun updateStatus(eventId: Long, status: String): AppResult<Unit>
    suspend fun addExtraClass(
        subjectId: Long,
        scheduledAt: Long,
        endAt: Long,
        linkedSlotId: Long?
    ): AppResult<Long>
}
