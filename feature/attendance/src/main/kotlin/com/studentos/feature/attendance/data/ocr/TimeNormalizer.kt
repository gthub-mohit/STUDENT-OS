package com.studentos.feature.attendance.data.ocr

/**
 * TimeNormalizer — Robust time parser and normalizer supporting diverse timetable formats:
 * - Dot and colon notations: 9.30, 9:30, 09.30, 09:00
 * - Hour ranges with shared or explicit AM/PM: 9-10 AM, 9 AM - 10 AM, 1 - 2 PM, 11 AM - 1 PM
 * - Cross-noon ranges: 11 AM - 1 PM, 11.00 - 1.00
 * - 24-hour military times: 14:00 - 15:00
 * - Period header and label extraction: Period 1, P1, Slot 1, 1st
 * - Resilient day of week detection ignoring surrounding punctuation and noise
 */
object TimeNormalizer {

    data class TimeRangeMatch(
        val startTime: String,
        val endTime: String,
        val matchRange: IntRange
    )

    private val timeRangeRegex = Regex(
        """(?i)(?:^|[\s(])(\d{1,2}(?:[:.]\d{2})?\s*(?:AM|PM)?)\s*[-–—to]+\s*(\d{1,2}(?:[:.]\d{2})?\s*(?:AM|PM)?)(?:[\s)]|$)"""
    )

    private val singleTimeRegex = Regex(
        """(?i)\b(\d{1,2}(?:[:.]\d{2})?\s*(?:AM|PM)?)\b"""
    )

    private val periodPattern = Regex(
        """^(?:Period|Slot|P)?\s*[-–:]?\s*([1-9]|1[0-2])(?:st|nd|rd|th)?$""",
        RegexOption.IGNORE_CASE
    )

    private val dayRegex = Regex(
        """\b(mon(?:day)?|tue(?:s(?:day)?)?|wed(?:nesday)?|thu(?:r(?:s(?:day)?)?)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\b""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Finds the first valid time range in [text] and returns both the normalized start/end times
     * and the character range of the match within [text].
     */
    fun findTimeRangeMatch(text: String): TimeRangeMatch? {
        val match = timeRangeRegex.find(text) ?: return null
        val rawStart = match.groupValues[1]
        val rawEnd = match.groupValues[2]

        val range = normalizeRange(rawStart, rawEnd) ?: return null
        return TimeRangeMatch(
            startTime = range.first,
            endTime = range.second,
            matchRange = match.range
        )
    }

    /**
     * Parses a time range from [text] returning Pair(startTime, endTime) in "HH:mm" 24h format.
     */
    fun parseTimeRange(text: String): Pair<String, String>? {
        return findTimeRangeMatch(text)?.let { it.startTime to it.endTime }
    }

    /**
     * Normalizes a pair of raw start and end time strings.
     */
    fun normalizeRange(rawStart: String, rawEnd: String): Pair<String, String>? {
        val (startH, startM, startAmPm) = parseComponents(rawStart) ?: return null
        val (endH, endM, endAmPm) = parseComponents(rawEnd) ?: return null

        if (startM !in 0..59 || endM !in 0..59) return null

        var sAmPm = startAmPm
        var eAmPm = endAmPm

        if (sAmPm == null && eAmPm != null) {
            if (eAmPm == "AM") {
                sAmPm = "AM"
            } else {
                if (startH in 1..6 && endH in 1..6) {
                    sAmPm = "PM"
                } else if (startH in 7..11) {
                    sAmPm = "AM"
                } else if (startH == 12) {
                    sAmPm = "PM"
                } else {
                    sAmPm = "PM"
                }
            }
        } else if (sAmPm != null && eAmPm == null) {
            if (sAmPm == "AM") {
                if (endH in 1..6 || (endH == 12 && startH <= 11)) {
                    eAmPm = "PM"
                } else {
                    eAmPm = "AM"
                }
            } else {
                eAmPm = "PM"
            }
        } else if (sAmPm == null && eAmPm == null) {
            if (startH <= 12 && endH <= 12) {
                if (startH in 1..6 && endH in 1..6) {
                    sAmPm = "PM"
                    eAmPm = "PM"
                } else if (startH in 7..11 && endH in 1..6) {
                    sAmPm = "AM"
                    eAmPm = "PM"
                } else if (startH == 12 && endH in 1..6) {
                    sAmPm = "PM"
                    eAmPm = "PM"
                } else {
                    sAmPm = "AM"
                    eAmPm = if (endH == 12) "PM" else "AM"
                }
            }
        }

        val start24Hour = to24Hour(startH, sAmPm)
        val end24Hour = to24Hour(endH, eAmPm)

        if (start24Hour !in 0..23 || end24Hour !in 0..23) return null

        val startTotal = start24Hour * 60 + startM
        val endTotal = end24Hour * 60 + endM
        if (startTotal >= endTotal) return null

        return String.format("%02d:%02d", start24Hour, startM) to String.format("%02d:%02d", end24Hour, endM)
    }

    /**
     * Normalizes a single time string (e.g. "9.30", "9:30", "9 AM", "14:00") into "HH:mm".
     */
    fun normalizeSingleTime(rawTime: String, contextAmPm: String? = null): String? {
        val (h, m, amPm) = parseComponents(rawTime) ?: return null
        if (m !in 0..59) return null

        val effectiveAmPm = amPm ?: contextAmPm
        val h24 = to24Hour(h, effectiveAmPm)
        if (h24 !in 0..23) return null

        return String.format("%02d:%02d", h24, m)
    }

    /**
     * Parses period number headers (e.g. "Period 1", "P1", "Slot 1", "1", "1st").
     */
    fun parsePeriodHeader(text: String): Int? {
        val clean = text.trim()
        val match = periodPattern.find(clean) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    /**
     * Parses a day of week (1=Monday ... 7=Sunday) from text, resilient to surrounding punctuation.
     */
    fun parseDayOfWeek(text: String): Int? {
        val match = dayRegex.find(text) ?: return null
        return when (match.groupValues[1].lowercase()) {
            "mon", "monday" -> 1
            "tue", "tues", "tuesday" -> 2
            "wed", "wednesday" -> 3
            "thu", "thur", "thurs", "thursday" -> 4
            "fri", "friday" -> 5
            "sat", "saturday" -> 6
            "sun", "sunday" -> 7
            else -> null
        }
    }

    private data class ParsedTimeComponents(
        val hour: Int,
        val minute: Int,
        val amPm: String?
    )

    private fun parseComponents(raw: String): ParsedTimeComponents? {
        val trimmed = raw.trim().uppercase()
        if (trimmed.isBlank()) return null

        val amPm = when {
            trimmed.contains("PM") -> "PM"
            trimmed.contains("AM") -> "AM"
            else -> null
        }

        val digitsPart = trimmed.replace("AM", "").replace("PM", "").trim()
        val (hour, minute) = when {
            digitsPart.contains(":") -> {
                val parts = digitsPart.split(":")
                val h = parts[0].trim().toIntOrNull() ?: return null
                val m = parts[1].trim().toIntOrNull() ?: return null
                h to m
            }
            digitsPart.contains(".") -> {
                val parts = digitsPart.split(".")
                val h = parts[0].trim().toIntOrNull() ?: return null
                val m = parts[1].trim().toIntOrNull() ?: return null
                h to m
            }
            else -> {
                val h = digitsPart.toIntOrNull() ?: return null
                h to 0
            }
        }

        return ParsedTimeComponents(hour, minute, amPm)
    }

    private fun to24Hour(hour: Int, amPm: String?): Int {
        if (hour >= 24) return hour
        return when (amPm) {
            "AM" -> if (hour == 12) 0 else hour
            "PM" -> if (hour < 12) hour + 12 else hour
            else -> hour
        }
    }
}
