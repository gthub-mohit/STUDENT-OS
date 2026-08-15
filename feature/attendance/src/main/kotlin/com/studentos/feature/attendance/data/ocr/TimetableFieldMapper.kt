package com.studentos.feature.attendance.data.ocr

import com.google.mlkit.vision.text.Text
import com.studentos.feature.attendance.data.ocr.model.OcrRect
import com.studentos.feature.attendance.data.ocr.model.OcrTextElement
import com.studentos.feature.attendance.domain.model.OcrResult
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import javax.inject.Inject

/**
 * TimetableFieldMapper — Unified field mapper converting OCR results into structured [ParsedTimetableSlot] instances.
 *
 * Dual-Mode Strategy:
 * 1. **Grid Mode**: Employs [GridTimetableParser] to parse 2D structured grids using bounding box coordinates.
 * 2. **Linear Fallback**: Parses free-form line-by-line timetable texts.
 * 3. **Validation**: Enforces [TimetableValidator] invariants across all modes.
 */
class TimetableFieldMapper @Inject constructor(
    private val gridParser: GridTimetableParser,
    private val validator: TimetableValidator
) {

    /**
     * Maps ML Kit [Text] vision result into an [OcrResult].
     */
    fun map(visionText: Text): OcrResult {
        val rawText = visionText.text
        if (rawText.isBlank()) {
            return OcrResult(slots = emptyList(), hasWarnings = true, rawText = "")
        }

        val elements = mutableListOf<OcrTextElement>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                val rect = if (box != null) {
                    OcrRect(box.left, box.top, box.right, box.bottom)
                } else {
                    OcrRect.ZERO
                }
                val confidence = line.confidence ?: 0.90f
                elements.add(OcrTextElement(text = line.text, rect = rect, confidence = confidence))
            }
        }

        return mapFromElements(elements, rawText)
    }

    /**
     * Maps a structured list of [OcrTextElement] instances into an [OcrResult].
     */
    fun mapFromElements(elements: List<OcrTextElement>, rawText: String = ""): OcrResult {
        if (elements.isEmpty()) {
            return OcrResult(slots = emptyList(), hasWarnings = true, rawText = rawText)
        }

        // Mode 1: Try Coordinate-Aware Grid Parser
        if (gridParser.canParseAsGrid(elements)) {
            val gridSlots = gridParser.parseGrid(elements)
            if (gridSlots.isNotEmpty()) {
                val hasWarnings = gridSlots.any { it.isLowConfidence }
                return OcrResult(
                    slots = gridSlots,
                    hasWarnings = hasWarnings,
                    rawText = rawText.ifBlank { elements.joinToString("\n") { it.text } }
                )
            }
        }

        // Mode 2: Linear Text Parser Fallback
        val textToParse = if (rawText.isNotBlank()) rawText else elements.joinToString("\n") { it.text }
        return mapFromRawText(textToParse)
    }

    /**
     * Maps raw text string lines into an [OcrResult] (linear fallback parser).
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

        val validSlots = validator.validateAndFilter(slots)
        val hasWarnings = validSlots.any { it.isLowConfidence } || validSlots.isEmpty()
        return OcrResult(slots = validSlots, hasWarnings = hasWarnings, rawText = rawText)
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
        val timeRegex = Regex("""(\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?)\s*[-–to]+\s*(\d{1,2}:\d{2}\s*(?:AM|PM|am|pm)?)""")
        val match = timeRegex.find(line) ?: return null

        val rawStart = match.groupValues[1]
        val rawEnd = match.groupValues[2]

        val startTime = normalizeTime(rawStart)
        val endTime = normalizeTime(rawEnd)

        val remainingText = line.replace(match.value, "").trim()
        if (remainingText.isBlank()) return null

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
