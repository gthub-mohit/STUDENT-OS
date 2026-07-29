package com.studentos.feature.attendance.domain.model

/**
 * OcrResult — Wraps the outcome of an OCR text recognition and field mapping pass.
 *
 * @param slots List of extracted timetable slots
 * @param hasWarnings True if any slot is marked low-confidence or if extraction had warnings
 * @param rawText Unparsed raw OCR text output
 */
data class OcrResult(
    val slots: List<ParsedTimetableSlot>,
    val hasWarnings: Boolean,
    val rawText: String
)
