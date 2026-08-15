package com.studentos.feature.intelligence.domain.repository

/**
 * Domain repository contract for toggling assignment completion from Focus items.
 */
interface FocusAssignmentRepository {
    suspend fun setAssignmentCompleted(assignmentId: Long, isCompleted: Boolean)
}

/**
 * Domain repository contract for updating class attendance status from Focus items.
 */
interface FocusAttendanceRepository {
    suspend fun setAttendanceStatus(classEventId: Long, isPresent: Boolean)
}

/**
 * Domain repository contract for updating DSA topic mastery from Focus items.
 */
interface FocusDsaRepository {
    suspend fun setTopicRevised(topicId: Long, isRevised: Boolean)
}

/**
 * Domain repository contract for completing project tasks from Focus items.
 */
interface FocusProjectRepository {
    suspend fun setProjectTaskCompleted(taskId: Long, isCompleted: Boolean)
}
