package com.studentos.feature.intelligence.data.repository

import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.dao.ProjectTaskDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.intelligence.domain.repository.FocusAssignmentRepository
import com.studentos.feature.intelligence.domain.repository.FocusAttendanceRepository
import com.studentos.feature.intelligence.domain.repository.FocusDsaRepository
import com.studentos.feature.intelligence.domain.repository.FocusProjectRepository
import java.time.Clock
import javax.inject.Inject

class FocusAssignmentRepositoryImpl @Inject constructor(
    private val assignmentDao: AssignmentDao,
    private val appEventBus: AppEventBus,
    private val clock: Clock
) : FocusAssignmentRepository {

    override suspend fun setAssignmentCompleted(assignmentId: Long, isCompleted: Boolean) {
        val nowMs = clock.millis()
        val newStatus = if (isCompleted) AssignmentEntity.STATUS_COMPLETED else AssignmentEntity.STATUS_PENDING
        assignmentDao.updateStatus(assignmentId, newStatus, nowMs)
        appEventBus.emit(AppEvent.AssignmentStatusChanged(assignmentId, newStatus))
    }
}

class FocusAttendanceRepositoryImpl @Inject constructor(
    private val classEventDao: ClassEventDao,
    private val appEventBus: AppEventBus,
    private val clock: Clock
) : FocusAttendanceRepository {

    override suspend fun setAttendanceStatus(classEventId: Long, isPresent: Boolean) {
        val nowMs = clock.millis()
        val newStatus = if (isPresent) "PRESENT" else "SCHEDULED"
        classEventDao.updateStatus(classEventId, newStatus, nowMs)
        appEventBus.emit(AppEvent.AttendanceMarked(classEventId, newStatus))
    }
}

class FocusDsaRepositoryImpl @Inject constructor(
    private val dsaTopicDao: DsaTopicDao,
    private val appEventBus: AppEventBus,
    private val clock: Clock
) : FocusDsaRepository {

    override suspend fun setTopicRevised(topicId: Long, isRevised: Boolean) {
        val nowMs = clock.millis()
        val revisionStatus = if (isRevised) "REVISED" else "IN_PROGRESS"
        val confidence = if (isRevised) 5 else 3
        dsaTopicDao.updateMastery(topicId, confidence, revisionStatus, null, null, nowMs)
        appEventBus.emit(AppEvent.DsaTopicUpdated(topicId))
    }
}

class FocusProjectRepositoryImpl @Inject constructor(
    private val projectTaskDao: ProjectTaskDao,
    private val appEventBus: AppEventBus,
    private val clock: Clock
) : FocusProjectRepository {

    override suspend fun setProjectTaskCompleted(taskId: Long, isCompleted: Boolean) {
        val nowMs = clock.millis()
        val completedAt = if (isCompleted) nowMs else null
        projectTaskDao.updateCompletedAt(taskId, completedAt)
        val task = projectTaskDao.getTaskById(taskId)
        val projectId = task?.projectId ?: 0L
        appEventBus.emit(AppEvent.ProjectTaskCompleted(taskId, projectId))
    }
}
