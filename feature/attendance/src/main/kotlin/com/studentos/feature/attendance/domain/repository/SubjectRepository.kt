package com.studentos.feature.attendance.domain.repository

import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.events.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * SubjectRepository — Domain repository contract for subject management.
 */
interface SubjectRepository {
    fun getActiveSubjects(): Flow<List<SubjectEntity>>
    fun getAllSubjectsIncludingArchived(): Flow<List<SubjectEntity>>
    fun getSubjectById(id: Long): Flow<SubjectEntity?>
    suspend fun archiveSubject(id: Long): AppResult<Unit>
}
