package com.studentos.feature.attendance.data.ocr

import com.google.mlkit.vision.text.Text
import com.studentos.feature.attendance.domain.model.OcrResult
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import javax.inject.Inject

/**
 * TimetableFieldMapper — Post-processing pass converting raw OCR text blocks or text lines
 * into structured [ParsedTimetableSlot] instances.
 */
class TimetableFieldMapper @Inject constructor() {

    /**
     * Maps ML Kit [Text] vision result into an [OcrResult].
     */
    fun map(visionText: Text): OcrResult {
        val rawText = visionText.text
        if (rawText.isBlank()) {
            return OcrResult(slots = emptyList(), hasWarnings = true, rawText = "")
        }

        val slots = mutableListOf<ParsedTimetableSlot>()
        var currentDay = 1 // Default Monday

        for (block in visionText.textBlocks) {
            val confidence = block.lines.mapNotNull { it.confidence }.average().toFloat().let {
                if (it.isNaN() || it <= 0.0f) 0.85f else it
            }

            for (line in block.lines) {
                val textLine = line.text.trim()
                val detectedDay = parseDayOfWeek(textLine)
                if (detectedDay != null) {
                    currentDay = detectedDay
                    continue
                }

                val slot = parseLineToSlot(textLine, currentDay, confidence)
                if (slot != null) {
                    slots.add(slot)
                }
            }
        }

        val hasWarnings = slots.any { it.isLowConfidence } || slots.isEmpty()
        return OcrResult(slots = slots, hasWarnings = hasWarnings, rawText = rawText)
    }

    /**
     * Maps raw text string lines into an [OcrResult] (used for deterministic unit testing).
     */
    fun mapFromRawText(rawText: String, defaultConfidence: Float = 0.85f): OcrResult {
        if (rawText.isBlank()) {
            return OcrResult(slots = emptyList(), hasWarnings = true, rawText = rawText)
        }

        val lines = rawText.lines()
        val slots = mutableListOf<ParsedTimetableSlot>()
        var currentDay = 1 // Default Monday

        for (line in lines) {
            val textLine = line.trim()
            if (textLine.isBlank()) continue

            val detectedDay = parseDayOfWeek(textLine)
            if (detectedDay != null) {
                currentDay = detectedDay
                continue
            }

            val slot = parseLineToSlot(textLine, currentDay, defaultConfidence)
            if (slot != null) {
                slots.add(slot)
            }
        }

        val hasWarnings = slots.any { it.isLowConfidence } || slots.isEmpty()
        return OcrResult(slots = slots, hasWarnings = hasWarnings, rawText = rawText)
    }

    private fun parseDayOfWeek(text: String): Int? {
        val lower = text.lowercase()
        return when {
            lower.contains("monday") || lower.contains("mon") -> 1
            lower.contains("tuesday") || lower.contains("tue") -> 2
            lower.contains("wednesday") || lower.contains("wed") -> 3
            lower.contains("thursday") || lower.contains("thu") -> 4
            lower.contains("friday") || lower.contains("fri") -> 5
            lower.contains("saturday") || lower.contains("sat") -> 6
            lower.contains("sunday") || lower.contains("sun") -> 7
            else -> null
        }
    }

    private fun parseLineToSlot(line: String, dayOfWeek: Int, confidence: Float): ParsedTimetableSlot? {
        // Regex matching times like "09:00 - 10:00" or "9:00 AM - 10:00 AM" or "09:00-10:00"
        val timeRegex = Regex("""(\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?)\s*[-–to]+\s*(\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?)""")
        val match = timeRegex.find(line) ?: return null

        val rawStart = match.groupValues[1]
        val rawEnd = match.groupValues[2]

        val startTime = normalizeTime(rawStart)
        val endTime = normalizeTime(rawEnd)

        // Remaining text after removing the time portion is subject and optional location
        val remainingText = line.replace(match.value, "").trim()
        if (remainingText.isBlank()) return null

        // Parse subject and location e.g. "Mathematics - Room 101" or "Physics (Lab 3)"
        val parts = remainingText.split(Regex("""[-–|()]""")).map { it.trim() }.filter { it.isNotBlank() }
        val subjectName = parts.firstOrNull() ?: remainingText
        val location = if (parts.size > 1) parts[1] else null

        val isLowConfidence = confidence < ParsedTimetableSlot.CONFIDENCE_THRESHOLD

        return ParsedTimetableSlot(
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            subjectName = subjectName,
            location = location,
            confidence = confidence,
            isLowConfidence = isLowConfidence
        )
    }

    private fun normalizeTime(rawTime: String): String {
        val clean = rawTime.trim().uppercase()
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")
        val timeOnly = clean.replace("AM", "").replace("PM", "").trim()

        val parts = timeOnly.split(":")
        if (parts.size != 2) return "09:00"

        var hour = parts[0].toIntOrNull() ?: 9
        val minute = parts[1].toIntOrNull() ?: 0

        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0

        return String.format("%02d:%02d", hour, minute)
    }
}
