package com.studentos.core.database.relation

/**
 * SubjectAttendanceSummary — Aggregated attendance statistics for a single subject.
 *
 * Returned by single-query attendance aggregate queries in [com.studentos.core.database.dao.ClassEventDao].
 */
data class SubjectAttendanceSummary(
    val subjectId: Long,
    val subjectName: String,
    val presentCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val holidayCount: Int,
    val extraPresentCount: Int,
    val totalHeldCount: Int,
    val percentage: Double
)
