package com.studentos.feature.attendance.data.ocr

import android.util.Log
import com.studentos.feature.attendance.data.ocr.model.OcrRect
import com.studentos.feature.attendance.data.ocr.model.OcrTextElement
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import javax.inject.Inject

/**
 * GridTimetableParser — Fully generic coordinate-aware timetable grid detector and cell parser.
 *
 * Architecture & Hardening:
 * 1. Continuous uniform vertical partitioning for Day Rows (Y-axis).
 * 2. Continuous uniform horizontal partitioning for Period Columns (X-axis).
 * 3. Dynamic course & classroom legend extraction (metadata only — never generates slots).
 * 4. Semantic token classification (Course vs. Classroom vs. Faculty vs. Boilerplate).
 * 5. Rejection of location-only cells without valid course identity (False positive < False negative).
 * 6. Multi-period span resolution for true merged cells based on normalized column overlap ratios.
 * 7. Comprehensive diagnostic logging for full auditability.
 */
class GridTimetableParser @Inject constructor(
    private val validator: TimetableValidator
) {

    data class DayRow(
        val dayOfWeek: Int,
        val top: Int,
        val bottom: Int,
        val centerY: Int,
        val labelRect: OcrRect
    )

    data class PeriodColumn(
        val index: Int,
        val startTime: String,
        val endTime: String,
        val left: Int,
        val right: Int,
        val centerX: Int
    )

    data class DynamicLegends(
        val courses: Map<String, String> = emptyMap(),
        val classrooms: Map<String, String> = emptyMap(),
        val faculty: Set<String> = emptySet()
    )

    /**
     * Determines whether the given OCR elements form a structured grid.
     */
    fun canParseAsGrid(elements: List<OcrTextElement>): Boolean {
        if (elements.size < 3) return false
        val days = detectDayRows(elements)
        val periods = detectPeriodColumns(elements, days.firstOrNull()?.top ?: Int.MAX_VALUE)
        return days.isNotEmpty() && periods.isNotEmpty()
    }

    /**
     * Parses OCR text elements using coordinate geometry.
     */
    fun parseGrid(elements: List<OcrTextElement>): List<ParsedTimetableSlot> {
        if (elements.isEmpty()) return emptyList()

        // 1. Detect Day Rows (Y-axis)
        val dayRows = detectDayRows(elements)
        if (dayRows.isEmpty()) {
            logDiag("No day rows detected")
            return emptyList()
        }
        val firstDayTop = dayRows.minOf { it.top }
        val lastDayBottom = dayRows.maxOf { it.bottom }

        // 2. Detect Period Columns (X-axis) above the first day row
        val periodColumns = detectPeriodColumns(elements, firstDayTop)
        if (periodColumns.isEmpty()) {
            logDiag("No period columns detected")
            return emptyList()
        }

        logDiag("Detected ${dayRows.size} day rows and ${periodColumns.size} period columns")

        // 3. Extract dynamic legends below the grid (strictly metadata)
        val legendElements = elements.filter { it.rect.top >= lastDayBottom - 10 }
        val legends = extractDynamicLegends(legendElements)
        logDiag("Extracted legends: ${legends.courses.size} courses, ${legends.classrooms.size} classrooms, ${legends.faculty.size} faculty")

        // 4. Grid horizontal boundary
        val gridLeft = periodColumns.minOf { it.left }
        val gridRight = periodColumns.maxOf { it.right }

        // 5. Filter elements belonging to the grid body (exclude header days and bottom legends)
        val gridBodyElements = elements.filter { elem ->
            val cy = elem.rect.centerY
            val cx = elem.rect.centerX
            cy in firstDayTop..lastDayBottom && cx >= gridLeft - 40 && cx <= gridRight + 40 &&
                !isDayLabel(elem.text)
        }

        val rawSlots = mutableListOf<ParsedTimetableSlot>()

        // 6. Process each day row
        for (dayRow in dayRows) {
            val rowElements = gridBodyElements.filter { elem ->
                elem.rect.centerY in dayRow.top..dayRow.bottom ||
                    verticalOverlap(elem.rect, dayRow.top, dayRow.bottom) > 0.35f
            }

            if (rowElements.isEmpty()) continue

            val daySlots = parseRowIntoSlots(dayRow, rowElements, periodColumns, legends)
            rawSlots.addAll(daySlots)
        }

        // 7. Validate, deduplicate, and sort chronologically
        val validSlots = validator.validateAndFilter(rawSlots)
            .distinctBy { "${it.dayOfWeek}_${it.startTime}_${it.subjectName}" }
            .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))

        logDiag("Grid parsing produced ${validSlots.size} valid timetable slots after validation")
        return validSlots
    }

    // ────────────────────────────────────────────────────────────────────────
    // Detection: Day Rows (Continuous Uniform Partition)
    // ────────────────────────────────────────────────────────────────────────

    private fun detectDayRows(elements: List<OcrTextElement>): List<DayRow> {
        val detectedDays = mutableListOf<Pair<Int, OcrTextElement>>()

        for (elem in elements) {
            val day = parseDayOfWeek(elem.text)
            if (day != null) {
                detectedDays.add(day to elem)
            }
        }

        if (detectedDays.isEmpty()) return emptyList()

        // Keep leftmost instance per day and sort by Y position
        val distinctDays = detectedDays
            .groupBy { it.first }
            .mapValues { (_, pairs) -> pairs.minByOrNull { it.second.rect.left }!!.second }
            .toList()
            .sortedBy { it.second.rect.centerY }

        if (distinctDays.isEmpty()) return emptyList()

        val avgRowPitch = if (distinctDays.size >= 2) {
            (distinctDays.last().second.rect.centerY - distinctDays.first().second.rect.centerY).toDouble() / (distinctDays.size - 1)
        } else {
            distinctDays.first().second.rect.height.toDouble().coerceAtLeast(60.0)
        }

        val dayRows = mutableListOf<DayRow>()
        for (i in distinctDays.indices) {
            val (dayOfWeek, elem) = distinctDays[i]
            val cy = elem.rect.centerY

            val top = if (i == 0) {
                (cy - avgRowPitch / 2).toInt()
            } else {
                (distinctDays[i - 1].second.rect.centerY + cy) / 2
            }

            val bottom = if (i == distinctDays.size - 1) {
                (cy + avgRowPitch / 2).toInt()
            } else {
                (cy + distinctDays[i + 1].second.rect.centerY) / 2
            }

            dayRows.add(DayRow(dayOfWeek, top, bottom, cy, elem.rect))
        }

        return dayRows
    }

    private fun isDayLabel(text: String): Boolean {
        return parseDayOfWeek(text) != null
    }

    private fun parseDayOfWeek(text: String): Int? {
        return TimeNormalizer.parseDayOfWeek(text)
    }

    private fun formatDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "MON"
            2 -> "TUE"
            3 -> "WED"
            4 -> "THU"
            5 -> "FRI"
            6 -> "SAT"
            7 -> "SUN"
            else -> "DAY$dayOfWeek"
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Detection: Period Columns (Continuous Uniform Partition)
    // ────────────────────────────────────────────────────────────────────────

    private fun detectPeriodColumns(elements: List<OcrTextElement>, headerBottomCutoff: Int): List<PeriodColumn> {
        val headerElements = elements.filter { it.rect.bottom <= headerBottomCutoff + 50 }

        // 1. Time range headers: "8:00 - 8:55", "09:00-09:55", "9.00 - 9.55", "9-10 AM", etc.
        val timeMatches = mutableListOf<Triple<String, String, OcrRect>>()

        for (elem in headerElements) {
            val rangeMatch = TimeNormalizer.findTimeRangeMatch(elem.text)
            if (rangeMatch != null) {
                timeMatches.add(Triple(rangeMatch.startTime, rangeMatch.endTime, elem.rect))
            }
        }

        // 2. Period number headers: "1", "2", "3" ... "10", "Period 1", "P1", "Slot 1"
        val periodNumberMatches = headerElements
            .mapNotNull { elem ->
                val pNum = TimeNormalizer.parsePeriodHeader(elem.text)
                if (pNum != null) pNum to elem else null
            }
            .sortedBy { it.second.rect.centerX }

        if (timeMatches.isNotEmpty()) {
            val sortedTimes = timeMatches.sortedBy { it.third.centerX }
            val colPitch = if (sortedTimes.size >= 2) {
                (sortedTimes.last().third.centerX - sortedTimes.first().third.centerX).toDouble() / (sortedTimes.size - 1)
            } else {
                sortedTimes.first().third.width.toDouble().coerceAtLeast(80.0)
            }

            val columns = mutableListOf<PeriodColumn>()
            for (i in sortedTimes.indices) {
                val (start, end, rect) = sortedTimes[i]
                val cx = rect.centerX

                val left = if (i == 0) {
                    (cx - colPitch / 2).toInt()
                } else {
                    (sortedTimes[i - 1].third.centerX + cx) / 2
                }

                val right = if (i == sortedTimes.size - 1) {
                    (cx + colPitch / 2).toInt()
                } else {
                    (cx + sortedTimes[i + 1].third.centerX) / 2
                }

                columns.add(
                    PeriodColumn(
                        index = i + 1,
                        startTime = start,
                        endTime = end,
                        left = left,
                        right = right,
                        centerX = cx
                    )
                )
            }
            return columns
        }

        // Fallback: If only period numbers exist without explicit times
        if (periodNumberMatches.isNotEmpty()) {
            val colPitch = if (periodNumberMatches.size >= 2) {
                (periodNumberMatches.last().second.rect.centerX - periodNumberMatches.first().second.rect.centerX).toDouble() / (periodNumberMatches.size - 1)
            } else {
                periodNumberMatches.first().second.rect.width.toDouble().coerceAtLeast(80.0)
            }

            val columns = mutableListOf<PeriodColumn>()
            for (i in periodNumberMatches.indices) {
                val (pNum, elem) = periodNumberMatches[i]
                val cx = elem.rect.centerX

                val left = if (i == 0) (cx - colPitch / 2).toInt() else (periodNumberMatches[i - 1].second.rect.centerX + cx) / 2
                val right = if (i == periodNumberMatches.size - 1) (cx + colPitch / 2).toInt() else (cx + periodNumberMatches[i + 1].second.rect.centerX) / 2

                val startHour = 7 + pNum
                val startStr = String.format("%02d:00", startHour)
                val endStr = String.format("%02d:55", startHour)

                columns.add(PeriodColumn(pNum, startStr, endStr, left, right, cx))
            }
            return columns
        }

        return emptyList()
    }

    // ────────────────────────────────────────────────────────────────────────
    // Dynamic Legend Extraction
    // ────────────────────────────────────────────────────────────────────────

    private fun extractDynamicLegends(legendElements: List<OcrTextElement>): DynamicLegends {
        if (legendElements.isEmpty()) return DynamicLegends()

        val courseMap = mutableMapOf<String, String>()
        val classroomMap = mutableMapOf<String, String>()
        val facultySet = mutableSetOf<String>()

        val subjectCodeRegex = Regex("""\b([A-Z]{2,4}\s*\d{3})\b""")
        val lines = legendElements.sortedBy { it.rect.top }

        for (elem in lines) {
            val text = elem.text.trim()

            val codeMatch = subjectCodeRegex.find(text)
            if (codeMatch != null) {
                val code = codeMatch.value.replace(" ", "")
                val remaining = text.replace(codeMatch.value, "").trim(' ', '-', '|', ':')
                if (remaining.isNotBlank() && remaining.length > 3) {
                    courseMap[code] = remaining
                }
            }

            val roomLegendMatch = Regex("""\b(WS|L\d{3}|C\d{3}|CC\s*Floor\d)\s*[-|:]?\s*([A-Za-z0-9:\s]{3,30})\b""").find(text)
            if (roomLegendMatch != null) {
                val key = roomLegendMatch.groupValues[1].trim()
                val value = roomLegendMatch.groupValues[2].trim()
                if (value.isNotBlank() && value != key) {
                    classroomMap[key] = value
                }
            }

            val facultyMatch = Regex("""\b([A-Z]{2,4})\s*[-|:]?\s*(?:Dr\.|Prof\.|Mr\.|Ms\.)\s*([A-Za-z\s]+)""").find(text)
            if (facultyMatch != null) {
                val initial = facultyMatch.groupValues[1].trim()
                facultySet.add(initial)
            }
        }

        return DynamicLegends(courses = courseMap, classrooms = classroomMap, faculty = facultySet)
    }

    // ────────────────────────────────────────────────────────────────────────
    // Subject-Based Cell Grouping & Span Resolution
    // ────────────────────────────────────────────────────────────────────────

    private fun parseRowIntoSlots(
        dayRow: DayRow,
        rowElements: List<OcrTextElement>,
        periodColumns: List<PeriodColumn>,
        legends: DynamicLegends
    ): List<ParsedTimetableSlot> {
        val slots = mutableListOf<ParsedTimetableSlot>()
        val sortedColumns = periodColumns.sortedBy { it.left }
        val avgColWidth = if (sortedColumns.isNotEmpty()) {
            sortedColumns.map { it.right - it.left }.average()
        } else {
            100.0
        }
        val dayName = formatDayName(dayRow.dayOfWeek)

        // 1. Identify distinct COURSE elements (strictly course codes or recognized course names)
        val subjectElements = mutableListOf<Pair<String, OcrTextElement>>()

        for (elem in rowElements) {
            val detectedCourse = extractCourseIdentity(elem.text, legends)
            if (detectedCourse != null) {
                subjectElements.add(detectedCourse to elem)
            }
        }

        // If valid course elements were identified in this row, process them
        if (subjectElements.isNotEmpty()) {
            val sortedSubjects = subjectElements.sortedBy { it.second.rect.centerX }

            for (i in sortedSubjects.indices) {
                val (subjCode, subjElem) = sortedSubjects[i]
                // STEP 1: Determine covered columns based on normalized column overlap.
                // A column is "covered" if >= MIN_OVERLAP_RATIO of the column
                // width is physically overlapped by the subject bbox.
                val subjLeft = subjElem.rect.left
                val subjRight = subjElem.rect.right

                val coveredColumns = sortedColumns.filter { col ->
                    val colWidth = (col.right - col.left).coerceAtLeast(1)
                    val overlapLeft = maxOf(subjLeft, col.left)
                    val overlapRight = minOf(subjRight, col.right)
                    val overlapWidth = (overlapRight - overlapLeft).coerceAtLeast(0)
                    val ratio = overlapWidth.toFloat() / colWidth
                    ratio >= MIN_OVERLAP_RATIO
                }

                val isSubjMerged = coveredColumns.size > 1

                val prevSubjX = if (i > 0) sortedSubjects[i - 1].second.rect.centerX else Int.MIN_VALUE
                val nextSubjX = if (i < sortedSubjects.size - 1) sortedSubjects[i + 1].second.rect.centerX else Int.MAX_VALUE

                // Collect elements belonging strictly to this course cell in X-space
                val maxAllowedDistance = if (isSubjMerged) {
                    val spanWidth = (coveredColumns.last().right - coveredColumns.first().left).toDouble()
                    spanWidth * 0.90
                } else {
                    avgColWidth * 0.70
                }
                val associatedElements = rowElements.filter { elem ->
                    val cx = elem.rect.centerX
                    val distToSubj = Math.abs(cx - subjElem.rect.centerX)
                    distToSubj <= maxAllowedDistance &&
                        (prevSubjX == Int.MIN_VALUE || (cx - prevSubjX) > (subjElem.rect.centerX - cx)) &&
                        (nextSubjX == Int.MAX_VALUE || (nextSubjX - cx) > (cx - subjElem.rect.centerX))
                }

                val minLeft = associatedElements.minOfOrNull { it.rect.left } ?: subjElem.rect.left
                val maxRight = associatedElements.maxOfOrNull { it.rect.right } ?: subjElem.rect.right

                // STEP 2: Determine Start Column & End Column
                val (startCol, endCol) = when {
                    coveredColumns.size > 1 -> coveredColumns.first() to coveredColumns.last()
                    coveredColumns.size == 1 -> coveredColumns.first() to coveredColumns.first()
                    else -> {
                        val closest = sortedColumns.minByOrNull { col ->
                            if (subjElem.rect.centerX in col.left..col.right) 0 else Math.abs(col.centerX - subjElem.rect.centerX)
                        } ?: sortedColumns.first()
                        closest to closest
                    }
                }

                val combinedText = associatedElements.joinToString("\n") { it.text }
                val location = extractLocation(combinedText, legends)

                val avgConfidence = associatedElements.map { it.confidence }.average().toFloat().let {
                    if (it.isNaN() || it <= 0.0f) 0.90f else it
                }

                val candidateSlot = ParsedTimetableSlot(
                    dayOfWeek = dayRow.dayOfWeek,
                    startTime = startCol.startTime,
                    endTime = endCol.endTime,
                    subjectName = subjCode,
                    location = location,
                    confidence = avgConfidence,
                    isLowConfidence = avgConfidence < ParsedTimetableSlot.CONFIDENCE_THRESHOLD
                )

                if (validator.isValid(candidateSlot)) {
                    logDiag("[TimetableOcr] $dayName | bbox=[$minLeft,$maxRight] | course=$subjCode | location=$location | span=${startCol.index}..${endCol.index} | time=${startCol.startTime}-${endCol.endTime} -> ACCEPT")
                    slots.add(candidateSlot)
                } else {
                    logDiag("[TimetableOcr] $dayName | bbox=[$minLeft,$maxRight] | course=$subjCode | location=$location -> REJECT: validator rejected slot")
                }
            }
            return slots
        }

        // Fallback: If no explicit course tokens, verify if any column contains a valid non-classroom course
        for (col in sortedColumns) {
            val colElements = rowElements.filter { elem ->
                elem.rect.centerX in col.left..col.right
            }
            if (colElements.isEmpty()) continue

            val combinedText = colElements.joinToString("\n") { it.text }
            val course = extractCourseIdentity(combinedText, legends)
            val location = extractLocation(combinedText, legends)

            val minLeft = colElements.minOf { it.rect.left }
            val maxRight = colElements.maxOf { it.rect.right }

            if (course == null) {
                logDiag("[TimetableOcr] $dayName | bbox=[$minLeft,$maxRight] | course=null | location=$location | col=${col.index} -> REJECT: location-only / no course identity")
                continue
            }

            val avgConfidence = colElements.map { it.confidence }.average().toFloat().let {
                if (it.isNaN() || it <= 0.0f) 0.90f else it
            }

            val candidateSlot = ParsedTimetableSlot(
                dayOfWeek = dayRow.dayOfWeek,
                startTime = col.startTime,
                endTime = col.endTime,
                subjectName = course,
                location = location,
                confidence = avgConfidence,
                isLowConfidence = avgConfidence < ParsedTimetableSlot.CONFIDENCE_THRESHOLD
            )

            if (validator.isValid(candidateSlot)) {
                logDiag("[TimetableOcr] $dayName | bbox=[$minLeft,$maxRight] | course=$course | location=$location | col=${col.index} | time=${col.startTime}-${col.endTime} -> ACCEPT")
                slots.add(candidateSlot)
            } else {
                logDiag("[TimetableOcr] $dayName | bbox=[$minLeft,$maxRight] | course=$course | location=$location -> REJECT: validator rejected slot")
            }
        }

        return slots
    }

    // ────────────────────────────────────────────────────────────────────────
    // Semantic Classification: Course vs. Classroom vs. Faculty vs. Metadata
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Extracts a clean Course / Subject identifier.
     * Returns null if text contains only classroom, faculty, time, or boilerplate tokens.
     */
    private fun extractCourseIdentity(
        text: String,
        legends: DynamicLegends
    ): String? {
        val tokens = text.lines().flatMap { it.split(Regex("""[\s,;|]+""")) }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return null

        // 1. Check Course Legend
        for (token in tokens) {
            val clean = token.replace(" ", "")
            if (legends.courses.containsKey(clean)) {
                return clean
            }
        }

        val subjectCodeRegex = Regex("""\b([A-Z]{2,5}\s*\d{2,4}[A-Z]?)\b""")

        // 2. Check Standard Course Code Pattern (excluding known classroom / faculty / boilerplate tokens)
        for (token in tokens) {
            val clean = token.replace(" ", "")
            if (subjectCodeRegex.matches(clean) &&
                !isClassroomToken(clean, legends) &&
                !isFacultyToken(clean, legends) &&
                !isBoilerplateToken(clean)
            ) {
                return clean
            }
        }

        val match = subjectCodeRegex.find(text)
        if (match != null) {
            val candidate = match.value.replace(" ", "")
            if (!isClassroomToken(candidate, legends) &&
                !isFacultyToken(candidate, legends) &&
                !isBoilerplateToken(candidate)
            ) {
                return candidate
            }
        }

        // 3. Generic course name fallback — STRICT
        // Only accept tokens that look like plausible multi-word course names
        // (e.g. "Solid Mechanics", "Data Structures"). Single unknown tokens,
        // faculty initials, random garbage, and ambiguous strings are REJECTED.
        // Principle: FALSE POSITIVE CLASS > FALSE NEGATIVE CLASS is unacceptable.
        for (line in text.lines().map { it.trim() }.filter { it.isNotBlank() }) {
            if (!isLocationLine(line, legends) && !isBoilerplateToken(line)) {
                // A generic course name must be a multi-word phrase or a
                // clearly identifiable name (>= 5 chars, starts uppercase,
                // contains lowercase letters indicating a real word).
                val cleanLine = line.split(Regex("""[\s,;|]+"""))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .filterNot { isClassroomToken(it, legends) }
                    .filterNot { isFacultyToken(it, legends) }
                    .filterNot { isBoilerplateToken(it) }
                    .filterNot { it.matches(Regex("""^\d{1,2}:\d{2}$""")) }
                    .filterNot { it.matches(Regex("""^\d+$""")) }
                    .filter { token -> token.any { it.isLetter() } }
                    .filter { token -> token.count { it.isLetterOrDigit() || it == '-' || it == '&' }.toFloat() / token.length >= 0.75f }

                if (cleanLine.isEmpty()) continue

                // If single token and ambiguous (e.g. "PKM", "SwK", "Xb4"), reject it
                if (cleanLine.size == 1 && isAmbiguousToken(cleanLine.first())) continue

                // If all tokens are short uppercase initials (e.g. "TN HN", "PKM SKK", "SKS VK"), reject it
                val allShortInitials = cleanLine.all { token ->
                    token.length <= 4 && token.all { it.isUpperCase() } && !token.any { it.isDigit() }
                }
                if (allShortInitials) continue

                // Require either a multi-word name or a single word that
                // clearly looks like a course name (>= 5 chars, has mixed case or uppercase word >= 6 chars).
                val candidateName = cleanLine.joinToString(" ")
                if (candidateName.length >= 5 && candidateName.first().isUpperCase()) {
                    if (candidateName.any { it.isLowerCase() } || cleanLine.size >= 2 || candidateName.length >= 6) {
                        return candidateName
                    }
                }
            }
        }

        return null
    }

    /**
     * Extracts and formats all Classroom / Location tokens present in text.
     */
    private fun extractLocation(
        text: String,
        legends: DynamicLegends
    ): String? {
        val foundRooms = mutableListOf<String>()

        // 1. Match from Dynamic Classroom Legend
        for ((key, value) in legends.classrooms) {
            val keyRegex = Regex("""\b${Regex.escape(key)}\b""", RegexOption.IGNORE_CASE)
            if (keyRegex.containsMatchIn(text)) {
                val resolved = value.ifBlank { key }
                if (!foundRooms.contains(resolved)) {
                    foundRooms.add(resolved)
                }
            }
        }

        // 2. Check individual lines for descriptive room phrases (e.g. "Robotics Lab", "Auditorium 1", "Room 401", "WS, L002")
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines) {
            if (isLocationLine(line, legends)) {
                val cleanedRoom = cleanLocationLine(line, legends)
                if (cleanedRoom.isNotBlank() && !foundRooms.contains(cleanedRoom)) {
                    foundRooms.add(cleanedRoom)
                }
            }
        }

        // 3. Match generic room / building patterns if not already found: C\d{3}, L\d{3}, CC Floor\d+, WS, Room \d+, Lab \d+, Workshop, etc.
        if (foundRooms.isEmpty()) {
            val roomRegex = Regex(
                """\b(C\d{3}|L\d{3}|CC\s*Floor\d+|WS|Room\s*\d+|Lab\s*\d*|Hall\s*\d*|Workshop|Building\s*\d*|Auditorium\s*\d*)\b""",
                RegexOption.IGNORE_CASE
            )
            val matches = roomRegex.findAll(text)
            for (m in matches) {
                val raw = m.value.trim()
                val resolved = legends.classrooms[raw] ?: legends.classrooms[raw.uppercase()] ?: raw
                if (!foundRooms.contains(resolved)) {
                    foundRooms.add(resolved)
                }
            }
        }

        return if (foundRooms.isNotEmpty()) {
            foundRooms.joinToString(", ")
        } else {
            null
        }
    }

    private fun isLocationLine(line: String, legends: DynamicLegends): Boolean {
        if (legends.classrooms.keys.any { line.contains(it, ignoreCase = true) }) return true
        if (legends.classrooms.values.any { line.contains(it, ignoreCase = true) }) return true

        val locationIndicators = Regex(
            """\b(?:C\d{3}|L\d{3}|WS|CC(?:\s*Floor\d+)?|Room\s*\d+|Lab\s*\d*|Hall\s*\d*|Workshop|Auditorium\s*\d*|Building\s*\d*|Block\s*[A-Z\d]+)\b""",
            RegexOption.IGNORE_CASE
        )
        return locationIndicators.containsMatchIn(line)
    }

    private fun cleanLocationLine(line: String, legends: DynamicLegends): String {
        var clean = line

        // Resolve any known legend keys inside the line
        for ((k, v) in legends.classrooms) {
            val kRegex = Regex("""\b${Regex.escape(k)}\b""", RegexOption.IGNORE_CASE)
            if (kRegex.containsMatchIn(clean) && v.isNotBlank() && v != k) {
                clean = clean.replace(kRegex, v)
            }
        }

        // Strip known faculty from legend
        for (f in legends.faculty) {
            clean = clean.replace(Regex("""\b${Regex.escape(f)}\b"""), "").trim()
        }

        clean = clean.replace(Regex("""\b(?:Dr\.|Prof\.|Mr\.|Ms\.)[A-Za-z]+\b"""), "").trim()
        clean = clean.replace(Regex("""\b[A-Z]{2,4}/[A-Z]{2,4}\b"""), "").trim()
        clean = clean.replace(Regex("""\b(?:Entire\s*class|Batch\s*[A-Z\d]*|ECE\d*/MAE\d*|MAE\d*|ECE\d*)\b""", RegexOption.IGNORE_CASE), "").trim()

        // Strip trailing faculty initials (e.g. "CC Floor2 TN" -> "CC Floor2", "C003 PKM" -> "C003", "C104 SwK" -> "C104")
        // Protect room keywords like LAB, ROOM, HALL, WS, CC, etc.
        val trailingTokens = clean.split(Regex("""\s+"""))
        if (trailingTokens.size > 1) {
            val lastToken = trailingTokens.last()
            val isProtectedRoomKeyword = lastToken.uppercase() in setOf("LAB", "ROOM", "HALL", "WS", "CC", "WORKSHOP", "AUDITORIUM")
            val isFacultyInitial = !isProtectedRoomKeyword && (
                lastToken.matches(Regex("""^[A-Z]{2,4}$""")) ||
                lastToken.matches(Regex("""^[A-Z][a-z][A-Z]$"""))
            )
            if (isFacultyInitial) {
                clean = trailingTokens.dropLast(1).joinToString(" ")
            }
        }

        return clean.trim(' ', ',', ';', '-', '|', ':')
    }

    private fun isClassroomToken(token: String, legends: DynamicLegends): Boolean {
        if (legends.classrooms.containsKey(token) || legends.classrooms.containsKey(token.uppercase())) return true
        if (legends.classrooms.values.any { it.equals(token, ignoreCase = true) }) return true

        val classroomPattern = Regex(
            """^(?:C\d{3}|L\d{3}|WS|CC(?:\s*Floor\d+)?|Room\s*\d+|Lab\s*\d*|Hall\s*\d*|Workshop|Auditorium\s*\d*|Building\s*\d*|Block\s*[A-Z\d]+)$""",
            RegexOption.IGNORE_CASE
        )
        return classroomPattern.matches(token)
    }

    private fun isFacultyToken(token: String, legends: DynamicLegends): Boolean {
        if (legends.faculty.contains(token) || legends.faculty.contains(token.uppercase())) return true

        val facultyPattern = Regex(
            """^(?:Dr\.|Prof\.|Mr\.|Ms\.)[A-Za-z]+$|^[A-Z]{2,4}/[A-Z]{2,4}$""",
            RegexOption.IGNORE_CASE
        )
        return facultyPattern.matches(token)
    }

    private fun isBoilerplateToken(token: String): Boolean {
        val boilerplatePattern = Regex(
            """^(?:entire|class|classes|batch\s*[A-Z\d]*|group\s*[A-Z\d]*|section\s*[A-Z\d]*|lecture|theory|tutorial|practical|unknown|\d{1,2}:\d{2})$""",
            RegexOption.IGNORE_CASE
        )
        return boilerplatePattern.matches(token)
    }

    /**
     * Returns true for tokens that are ambiguous and CANNOT be confidently
     * identified as course names. These should be rejected rather than promoted
     * to subject names.
     *
     * Ambiguous tokens include:
     * - All-uppercase ≤ 4 chars without digits (likely faculty initials: PKM, HN, TN)
     * - Mixed-case initial patterns (e.g. SwK, AbC)
     * - Pure digits or digit-heavy short tokens
     * - Tokens with special characters / punctuation only
     * - Very short tokens (≤ 2 chars)
     */
    private fun isAmbiguousToken(token: String): Boolean {
        if (token.length <= 2) return true
        if (token.all { it.isDigit() }) return true
        if (!token.any { it.isLetter() }) return true

        // All-uppercase token without any digit, ≤ 4 chars → likely faculty initial (PKM, HN, TN, SG)
        if (token.length <= 4 && token.all { it.isUpperCase() || !it.isLetter() } && !token.any { it.isDigit() }) {
            return true
        }

        // Mixed-case initial patterns like SwK, AbC (capital-lower-capital)
        if (token.length <= 4 && token.matches(Regex("""^[A-Z][a-z][A-Z][a-z]?$"""))) {
            return true
        }

        // Mostly non-alphanumeric garbage
        val alphanumCount = token.count { it.isLetterOrDigit() }
        if (alphanumCount.toFloat() / token.length < 0.5f) return true

        return false
    }

    private fun verticalOverlap(rect: OcrRect, top: Int, bottom: Int): Float {
        val overlapTop = Math.max(rect.top, top)
        val overlapBottom = Math.min(rect.bottom, bottom)
        val overlapHeight = (overlapBottom - overlapTop).coerceAtLeast(0)
        return if (rect.height > 0) overlapHeight.toFloat() / rect.height else 0f
    }

    private fun normalizeTime(rawTime: String): String {
        return TimeNormalizer.normalizeSingleTime(rawTime) ?: "08:00"
    }

    private fun logDiag(message: String) {
        try {
            Log.d("TimetableOcr", message)
        } catch (_: Exception) {}
    }

    companion object {
        /**
         * Minimum fraction of a period column's width that must be overlapped
         * by the subject bounding box for the column to be considered "covered"
         * in a merged cell span.
         */
        const val MIN_OVERLAP_RATIO = 0.25f
    }
}
