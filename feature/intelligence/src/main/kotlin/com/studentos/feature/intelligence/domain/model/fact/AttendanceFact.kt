package com.studentos.feature.intelligence.domain.model.fact

data class TimetableSlotFact(
    val slotId: Long,
    val subjectId: Long,
    val subjectName: String,
    val startTime: String,
    val endTime: String,
    val status: String? = null
)

data class LowAttendanceSubjectFact(
    val subjectId: Long,
    val subjectName: String,
    val currentPercentage: Double,
    val targetPercentage: Double,
    val isCritical: Boolean // true if below cutoff
)

data class AttendanceFact(
    val todaySlots: List<TimetableSlotFact> = emptyList(),
    val lowAttendanceSubjects: List<LowAttendanceSubjectFact> = emptyList(),
    val mustAttendSubjectIds: List<Long> = emptyList(),
    val canSkipSubjectIds: List<Long> = emptyList()
)
