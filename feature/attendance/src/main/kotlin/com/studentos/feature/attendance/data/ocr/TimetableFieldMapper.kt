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
        return TimeNormalizer.parseDayOfWeek(text)
    }

    private fun parseLineToSlot(line: String, dayOfWeek: Int, confidence: Float): ParsedTimetableSlot? {
        val timeMatch = TimeNormalizer.findTimeRangeMatch(line)
        if (timeMatch != null) {
            val startTime = timeMatch.startTime
            val endTime = timeMatch.endTime

            var remainingText = line.removeRange(timeMatch.matchRange).trim()
            if (remainingText.isBlank()) return null

            // Clean any leading period prefix, e.g. "Period 1:", "P1 -", "1. "
            remainingText = remainingText.replace(Regex("""^(?:Period|Slot|P)\s*[-–:]?\s*\d+\s*[-–:)]*""", RegexOption.IGNORE_CASE), "").trim()
            remainingText = remainingText.replace(Regex("""^\d+\.\s*"""), "").trim()
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

        // Check if line represents a safe period-number format without explicit times
        // e.g. "Period 1: Mathematics - Room 101" or "P2 Mathematics"
        val periodRegex = Regex("""^(?:Period|Slot|P)\s*[-–:]?\s*([1-9]|1[0-2])\b\s*[-–:(]*\s*(.+)$""", RegexOption.IGNORE_CASE)
        val periodMatch = periodRegex.find(line)
        if (periodMatch != null) {
            val pNum = periodMatch.groupValues[1].toIntOrNull() ?: return null
            val rawSubjectContent = periodMatch.groupValues[2].trim(' ', '-', '–', ':', ')', '(')
            if (rawSubjectContent.isBlank()) return null

            val parts = rawSubjectContent.split(Regex("""[-–|()]""")).map { it.trim() }.filter { it.isNotBlank() }
            val subjectName = parts.firstOrNull() ?: rawSubjectContent
            val location = if (parts.size > 1) parts[1] else null

            val startHour = 7 + pNum
            val startTime = String.format("%02d:00", startHour)
            val endTime = String.format("%02d:55", startHour)

            // Period-derived times without explicit clock times are flagged as low confidence
            // so they are surfaced in the review/correction flow
            val effectiveConfidence = minOf(confidence, 0.70f)

            return ParsedTimetableSlot(
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                subjectName = subjectName,
                location = location,
                confidence = effectiveConfidence,
                isLowConfidence = true
            )
        }

        return null
    }

    private fun normalizeTime(rawTime: String): String {
        return TimeNormalizer.normalizeSingleTime(rawTime) ?: "09:00"
    }
}
