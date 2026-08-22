package com.studentos.feature.attendance.data.repository

import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.core.events.AppError
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppResult
import com.studentos.core.notifications.scheduler.NotificationRescheduler
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * ClassEventRepositoryImpl — Data repository implementing [ClassEventRepository].
 */
class ClassEventRepositoryImpl @Inject constructor(
    private val classEventDao: ClassEventDao,
    private val appEventBus: AppEventBus,
    private val notificationRescheduler: NotificationRescheduler? = null
) : ClassEventRepository {

    override fun getEventsForSubject(subjectId: Long): Flow<List<ClassEventEntity>> {
        return classEventDao.getEventsForSubject(subjectId)
    }

    override fun getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> {
        return classEventDao.getEventsForDay(startEpoch, endEpoch)
    }

    override fun getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> {
        return classEventDao.getEventsForWeek(startEpoch, endEpoch)
    }

    override fun getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>> {
        return classEventDao.getAllAttendanceSummaries()
    }

    override suspend fun updateStatus(eventId: Long, status: String): AppResult<Unit> {
        return try {
            val updatedAt = System.currentTimeMillis()
            classEventDao.updateStatus(eventId, status, updatedAt)

            val event = classEventDao.getEventByIdOnce(eventId)
                ?: return AppResult.Failure(AppError.ValidationError("Class event not found"))

            // Emit events AFTER database write completes successfully
            appEventBus.emit(AppEvent.AttendanceMarked(subjectId = event.subjectId, status = status))
            appEventBus.emit(AppEvent.AttendanceUpdated(subjectId = event.subjectId))

            try {
                notificationRescheduler?.rescheduleClassReminders()
            } catch (_: Exception) {
                // Non-blocking
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to update event status"))
        }
    }

    override suspend fun addExtraClass(
        subjectId: Long,
        scheduledAt: Long,
        endAt: Long,
        linkedSlotId: Long?
    ): AppResult<Long> {
        return try {
            val now = System.currentTimeMillis()
            val extraClassEntity = ClassEventEntity(
                subjectId = subjectId,
                linkedSlotId = linkedSlotId,
                scheduledAt = scheduledAt,
                endAt = endAt,
                status = ClassEventEntity.STATUS_EXTRA_CLASS,
                isExtra = true,
                updatedAt = now
            )

            val newId = classEventDao.insert(extraClassEntity)

            // Emit events AFTER database write completes successfully
            appEventBus.emit(
                AppEvent.AttendanceMarked(
                    subjectId = subjectId,
                    status = ClassEventEntity.STATUS_EXTRA_CLASS
                )
            )
            appEventBus.emit(AppEvent.AttendanceUpdated(subjectId = subjectId))

            try {
                notificationRescheduler?.rescheduleClassReminders()
            } catch (_: Exception) {
                // Non-blocking
            }

            AppResult.Success(newId)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to add extra class"))
        }
    }
}
