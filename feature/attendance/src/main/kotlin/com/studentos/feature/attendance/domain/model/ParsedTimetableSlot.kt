package com.studentos.feature.attendance.domain.model

/**
 * ParsedTimetableSlot — Represents an extracted timetable slot produced by OCR field mapping.
 *
 * @param dayOfWeek Day of week (1 = Monday, 7 = Sunday)
 * @param startTime 24-hour HH:mm start time string e.g. "09:00"
 * @param endTime 24-hour HH:mm end time string e.g. "10:00"
 * @param subjectName Name of the academic subject
 * @param location Optional classroom or hall location e.g. "Room 101"
 * @param confidence Field-level OCR confidence score between 0.0f and 1.0f
 * @param isLowConfidence Flagged true if confidence < 0.80f threshold
 */
data class ParsedTimetableSlot(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val subjectName: String,
    val location: String? = null,
    val confidence: Float = 1.0f,
    val isLowConfidence: Boolean = false
) {
    companion object {
        const val CONFIDENCE_THRESHOLD = 0.80f
    }
}
