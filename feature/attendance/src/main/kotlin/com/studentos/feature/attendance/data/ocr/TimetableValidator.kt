package com.studentos.feature.attendance.data.ocr

import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import javax.inject.Inject

/**
 * TimetableValidator — Production-hardened validator enforcing strict domain invariants.
 *
 * Invariants:
 * 1. Day of week must be 1..7.
 * 2. Subject name must be non-blank and represent a valid course identity.
 * 3. Subject name must NOT be a time string (e.g. "13:00", "13:00-13:55").
 * 4. Subject name must NOT be a classroom/location code (e.g. "C003", "C104", "WS", "Room 201", "Lab 1").
 * 5. Subject name must NOT be boilerplate/metadata (e.g. "Entire class", "Batch A", "Unknown").
 * 6. Location must NOT contain invalid minutes (e.g. ":65").
 * 7. Start time must be strictly before End time with valid 0..23 hours and 0..59 minutes.
 */
class TimetableValidator @Inject constructor() {

    private val timePattern = Regex("""^\d{1,2}:\d{2}(?:\s*[-–to]+\s*\d{1,2}:\d{2})?$""")

    private val locationAsSubjectPattern = Regex(
        """^(?:C\d{3}|L\d{3}|WS|CC(?:\s*Floor\d+)?|Room\s*\d+|Lab\s*\d*|Hall\s*\d*|Workshop(?::\s*Mechanical)?|Auditorium|Building\s*\d*|Block\s*[A-Z\d]+)$""",
        RegexOption.IGNORE_CASE
    )

    private val boilerplatePattern = Regex(
        """^(?:entire\s*class|classes|batch\s*[A-Z\d]*|group\s*[A-Z\d]*|section\s*[A-Z\d]*|lecture|theory|tutorial|practical|unknown|courses?|classrooms?|faculty|junio\w*)$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Pattern matching faculty initials: 2-4 uppercase letters without any digit.
     * Examples: PKM, HN, TN, SG, SKK
     */
    private val facultyInitialPattern = Regex(
        """^[A-Z]{2,4}$"""
    )

    /**
     * Pattern matching mixed-case initials like SwK, AbC.
     */
    private val mixedCaseInitialPattern = Regex(
        """^[A-Z][a-z][A-Z][a-z]?$"""
    )

    /**
     * Pattern matching faculty slash patterns like ECE2/MAE2, SKS/VK.
     */
    private val slashPattern = Regex(
        """^[A-Z\d]{2,5}/[A-Z\d]{2,5}$"""
    )

    /**
     * Validates a single [ParsedTimetableSlot]. Returns true if valid, false if invalid.
     */
    fun isValid(slot: ParsedTimetableSlot): Boolean {
        // 1. Day of week check
        if (slot.dayOfWeek !in 1..7) return false

        // 2. Subject name check
        val subject = slot.subjectName.trim()
        if (subject.isBlank()) return false
        if (subject.length < 2) return false
        if (timePattern.matches(subject)) return false
        if (subject.all { it.isDigit() || it.isWhitespace() || it == ':' || it == '-' || it == '–' }) return false
        if (boilerplatePattern.matches(subject)) return false
        if (locationAsSubjectPattern.matches(subject)) return false
        if (facultyInitialPattern.matches(subject)) return false
        if (mixedCaseInitialPattern.matches(subject)) return false
        if (slashPattern.matches(subject)) return false

        // Must have at least one alphabetic character
        if (!subject.any { it.isLetter() }) return false

        // 3. Location check
        val loc = slot.location?.trim()
        if (loc != null) {
            if (timePattern.matches(loc)) return false
            if (hasInvalidMinutes(loc)) return false
        }

        // 4. Time format check
        val startMinutes = parseMinutes(slot.startTime) ?: return false
        val endMinutes = parseMinutes(slot.endTime) ?: return false

        // 5. Start time must be strictly before end time
        if (startMinutes >= endMinutes) return false

        return true
    }

    /**
     * Filters a list of slots, keeping only valid ones.
     */
    fun validateAndFilter(slots: List<ParsedTimetableSlot>): List<ParsedTimetableSlot> {
        return slots.filter { isValid(it) }
    }

    private fun parseMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val min = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || min !in 0..59) return null
        return hour * 60 + min
    }

    private fun hasInvalidMinutes(text: String): Boolean {
        val minuteMatch = Regex(""":(\d{2})""").findAll(text)
        for (match in minuteMatch) {
            val min = match.groupValues[1].toIntOrNull() ?: continue
            if (min >= 60) return true
        }
        return false
    }
}
