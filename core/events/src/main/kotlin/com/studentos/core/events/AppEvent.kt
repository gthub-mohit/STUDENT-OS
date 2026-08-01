package com.studentos.core.events

/**
 * AppEvent — Sealed class hierarchy representing cross-module domain events emitted to [AppEventBus].
 */
sealed class AppEvent {
    data class AttendanceMarked(val subjectId: Long, val status: String) : AppEvent()
    data class AttendanceUpdated(val subjectId: Long) : AppEvent()
    data class AssignmentStatusChanged(val assignmentId: Long, val newStatus: String) : AppEvent()
    data class AssignmentCreated(val assignmentId: Long) : AppEvent()
    data class AssignmentDeleted(val assignmentId: Long) : AppEvent()
    data class ProjectTaskCompleted(val taskId: Long, val projectId: Long) : AppEvent()
    data class ProjectUpdated(val projectId: Long) : AppEvent()
    data object CpSyncCompleted : AppEvent()
    data class ContestReflectionAdded(val contestId: Long) : AppEvent()
    data class DsaTopicUpdated(val topicId: Long) : AppEvent()
    data class DailyScoreChanged(val newScore: Int) : AppEvent()
}
