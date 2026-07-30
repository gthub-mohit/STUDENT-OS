package com.studentos.feature.assignments.domain.scheduler

import com.studentos.core.database.entity.AssignmentEntity

/**
 * AssignmentReminderScheduler — Domain contract for WorkManager reminder scheduling.
 */
interface AssignmentReminderScheduler {
    suspend fun scheduleReminder(assignment: AssignmentEntity)
    suspend fun cancelReminder(assignmentId: Long)
}
