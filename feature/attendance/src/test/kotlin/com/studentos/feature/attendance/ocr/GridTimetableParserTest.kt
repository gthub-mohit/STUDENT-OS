package com.studentos.feature.attendance.ocr

import com.studentos.feature.attendance.data.ocr.GridTimetableParser
import com.studentos.feature.attendance.data.ocr.TimetableValidator
import com.studentos.feature.attendance.data.ocr.model.OcrRect
import com.studentos.feature.attendance.data.ocr.model.OcrTextElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridTimetableParserTest {

    private val validator = TimetableValidator()
    private val parser = GridTimetableParser(validator)

    @Test
    fun parseGrid_fullReferenceTimetable_extractsAllSlotsCorrectly() {
        // Grid setup: 10 columns across X (100 to 1100), 5 days across Y (100 to 600)
        // Col width = 100, Row height = 100
        val elements = mutableListOf<OcrTextElement>()

        // 1. Period Headers (Y: 20..80)
        val periodTimes = listOf(
            "8:00 - 8:55",
            "9:00 - 9:55",
            "10:00 - 10:55",
            "11:00 - 11:55",
            "12:00 - 12:55",
            "13:00 - 13:55",
            "14:00 - 14:55",
            "15:00 - 15:55",
            "16:00 - 16:55",
            "17:00 - 17:55"
        )
        for (i in 0 until 10) {
            val left = 100 + i * 100
            val right = left + 100
            elements.add(OcrTextElement((i + 1).toString(), OcrRect(left + 40, 20, left + 60, 40)))
            elements.add(OcrTextElement(periodTimes[i], OcrRect(left + 5, 45, right - 5, 75)))
        }

        // 2. Day Labels (X: 10..80)
        val days = listOf("Mon" to 1, "Tue" to 2, "Wed" to 3, "Thu" to 4, "Fri" to 5)
        for ((idx, dayPair) in days.withIndex()) {
            val top = 100 + idx * 100
            elements.add(OcrTextElement(dayPair.first, OcrRect(10, top + 30, 80, top + 70)))
        }

        // Helper to add cell text
        fun addCell(dayIdx: Int, startCol: Int, endCol: Int, text: String) {
            val top = 100 + dayIdx * 100 + 10
            val bottom = top + 80
            val left = 100 + (startCol - 1) * 100 + 5
            val right = 100 + endCol * 100 - 5
            elements.add(OcrTextElement(text, OcrRect(left, top, right, bottom)))
        }

        // 3. Monday (dayIdx = 0)
        addCell(0, 2, 2, "Entire class\nME201\nC003 PKM")
        addCell(0, 3, 3, "Entire class\nMA203\nC104 HN")
        addCell(0, 8, 8, "Entire class\nVAC202\nC104 SKS/VK")
        addCell(0, 9, 9, "Entire class\nCS203\nC104 SG")
        addCell(0, 10, 10, "Entire class\nCS207\nC104 SwK")

        // 4. Tuesday (dayIdx = 1) — Merged Periods 1-3 (ME201) & 9-10 (CS203)
        addCell(1, 1, 3, "Lab2\nME201\nPKM\nWS, L002")
        addCell(1, 4, 4, "Entire class\nME202\nC003 SKK")
        addCell(1, 7, 7, "Entire class\nMA203\nC104 HN")
        addCell(1, 8, 8, "ECE2/MAE2\nCS207\nC104 SwK")
        addCell(1, 9, 10, "CS203\nCC Floor3, CC Floor2 SG")

        // 5. Wednesday (dayIdx = 2)
        addCell(2, 2, 2, "Entire class\nVAC202\nC104 SKS/VK")
        addCell(2, 3, 3, "Entire class\nME201\nC003 PKM")
        addCell(2, 4, 4, "Entire class\nME224\nC003 TN")
        addCell(2, 8, 8, "Entire class\nCS207\nC104 SwK")
        addCell(2, 9, 9, "Entire class\nCS203\nC104 SG")
        addCell(2, 10, 10, "Entire class\nME202\nC003 SKK")

        // 6. Thursday (dayIdx = 3) — Merged Periods 7-9 (ME224)
        addCell(3, 2, 2, "Entire class\nME201\nC003 PKM")
        addCell(3, 4, 4, "Entire class\nME224\nC003 TN")
        addCell(3, 5, 5, "Entire class\nME202\nC003 SKK")
        addCell(3, 7, 9, "MAE2\nME224\nCC Floor2 TN")
        addCell(3, 10, 10, "Entire class\nMA203\nC104 HN")

        // 7. Friday (dayIdx = 4) — Merged Periods 1-3 (ME201)
        addCell(4, 1, 3, "Lab1\nME201\nPKM\nWS, L002")
        addCell(4, 4, 4, "Entire class\nME202\nC002 SKK")
        addCell(4, 5, 5, "Entire class\nME224\nC003 TN")
        addCell(4, 7, 7, "Entire class\nMA203\nC104 HN")
        addCell(4, 8, 8, "Entire class\nCS207\nC104 SwK")
        addCell(4, 9, 9, "Entire class\nCS203\nC104 SG")

        // 8. Legend at bottom (Y: 650..800)
        val legendY = 650
        elements.add(OcrTextElement("Courses", OcrRect(50, legendY, 200, legendY + 20)))
        elements.add(OcrTextElement("CS203 Object Oriented Programming Concepts", OcrRect(50, legendY + 25, 300, legendY + 45)))
        elements.add(OcrTextElement("CS207 Computer Organization and Architecture", OcrRect(50, legendY + 50, 300, legendY + 70)))
        elements.add(OcrTextElement("MA203 Probability, Statistics and Random Process", OcrRect(50, legendY + 75, 300, legendY + 95)))
        elements.add(OcrTextElement("ME201 Solid Mechanics", OcrRect(50, legendY + 100, 300, legendY + 120)))
        elements.add(OcrTextElement("ME202 Thermodynamics", OcrRect(50, legendY + 125, 300, legendY + 145)))
        elements.add(OcrTextElement("ME224 Theory of Machines", OcrRect(50, legendY + 150, 300, legendY + 170)))
        elements.add(OcrTextElement("VAC202 EVS", OcrRect(50, legendY + 175, 300, legendY + 195)))

        elements.add(OcrTextElement("Classrooms", OcrRect(400, legendY, 550, legendY + 20)))
        elements.add(OcrTextElement("WS Workshop: Mechanical", OcrRect(400, legendY + 25, 650, legendY + 45)))
        elements.add(OcrTextElement("C104 C104", OcrRect(400, legendY + 50, 650, legendY + 70)))

        elements.add(OcrTextElement("Faculty / Junior Technical Staff", OcrRect(700, legendY, 900, legendY + 20)))
        elements.add(OcrTextElement("HN Dr. Himadri Nayak", OcrRect(700, legendY + 25, 900, legendY + 45)))
        elements.add(OcrTextElement("PKM Dr. Purnendu Kumar Mandal", OcrRect(700, legendY + 50, 900, legendY + 70)))

        // ACT
        assertTrue(parser.canParseAsGrid(elements))
        val slots = parser.parseGrid(elements)

        // ASSERT
        // Check total slots: 5 (Mon) + 5 (Tue) + 6 (Wed) + 5 (Thu) + 6 (Fri) = 27 slots
        assertEquals(27, slots.size)

        // 1. Monday check: ME201 = 09:00-09:55 and retain class immediately following (MA203 = 10:00-10:55)
        val monSlots = slots.filter { it.dayOfWeek == 1 }
        assertEquals(5, monSlots.size)
        assertEquals("ME201", monSlots[0].subjectName)
        assertEquals("09:00", monSlots[0].startTime)
        assertEquals("09:55", monSlots[0].endTime)
        assertEquals("C003", monSlots[0].location)

        assertEquals("MA203", monSlots[1].subjectName)
        assertEquals("10:00", monSlots[1].startTime)
        assertEquals("10:55", monSlots[1].endTime)
        assertEquals("C104", monSlots[1].location)

        // 2. Tuesday Merged Periods 1-3 check (ME201 = 08:00 - 10:55)
        val tueMerged1 = slots.firstOrNull { it.dayOfWeek == 2 && it.subjectName == "ME201" }
        assertNotNull(tueMerged1)
        assertEquals("08:00", tueMerged1!!.startTime)
        assertEquals("10:55", tueMerged1.endTime)
        assertTrue(tueMerged1.location?.contains("Workshop: Mechanical") == true)

        // Tuesday Merged Periods 9-10 check (CS203 = 16:00 - 17:55)
        val tueMerged2 = slots.firstOrNull { it.dayOfWeek == 2 && it.subjectName == "CS203" }
        assertNotNull(tueMerged2)
        assertEquals("16:00", tueMerged2!!.startTime)
        assertEquals("17:55", tueMerged2.endTime)
        assertTrue(tueMerged2.location?.contains("CC Floor") == true)

        // 3. Wednesday check: VAC202 = 09:00-09:55
        val wedVac = slots.firstOrNull { it.dayOfWeek == 3 && it.subjectName == "VAC202" }
        assertNotNull(wedVac)
        assertEquals("09:00", wedVac!!.startTime)
        assertEquals("09:55", wedVac.endTime)

        val wedMe201 = slots.firstOrNull { it.dayOfWeek == 3 && it.subjectName == "ME201" }
        assertNotNull(wedMe201)
        assertEquals("10:00", wedMe201!!.startTime)
        assertEquals("10:55", wedMe201.endTime)

        // 4. Thursday check: ME201 = 09:00-09:55 and must not be dropped
        val thuMe201 = slots.firstOrNull { it.dayOfWeek == 4 && it.subjectName == "ME201" }
        assertNotNull(thuMe201)
        assertEquals("09:00", thuMe201!!.startTime)
        assertEquals("09:55", thuMe201.endTime)

        // Thursday Merged Periods 7-9 check (ME224 = 14:00 - 16:55)
        val thuMerged = slots.firstOrNull { it.dayOfWeek == 4 && it.subjectName == "ME224" && it.startTime == "14:00" }
        assertNotNull(thuMerged)
        assertEquals("14:00", thuMerged!!.startTime)
        assertEquals("16:55", thuMerged.endTime)
        assertEquals("CC Floor2", thuMerged.location)

        // 5. Friday Merged Periods 1-3 check (ME201 = 08:00 - 10:55)
        val friMerged = slots.firstOrNull { it.dayOfWeek == 5 && it.subjectName == "ME201" }
        assertNotNull(friMerged)
        assertEquals("08:00", friMerged!!.startTime)
        assertEquals("10:55", friMerged.endTime)
        assertTrue(friMerged.location?.contains("Workshop: Mechanical") == true)
    }

    @Test
    fun parseGrid_locationOnlyCell_producesNoSlot() {
        // Cells with only classroom codes (e.g. C003, C104, WS, Lab1) must NEVER become classes
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 10:00", OcrRect(200, 30, 300, 60)),
            OcrTextElement("Mon", OcrRect(10, 100, 80, 150)),

            // Period 1 has only a classroom code (C003)
            OcrTextElement("C003", OcrRect(105, 105, 195, 145)),
            // Period 2 has a valid course (ME201) with location (C104)
            OcrTextElement("ME201\nC104", OcrRect(205, 105, 295, 145))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(1, slots.size)
        assertEquals("ME201", slots[0].subjectName)
        assertEquals("C104", slots[0].location)
        assertEquals("09:00", slots[0].startTime)
        assertEquals("10:00", slots[0].endTime)
    }

    @Test
    fun parseGrid_locationOnlyCellWithMultipleRooms_producesNoSlot() {
        // Cells with multiple rooms like "C003 / L002" or "WS" without course must produce NO slot
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 10:00", OcrRect(200, 30, 300, 60)),
            OcrTextElement("Tue", OcrRect(10, 100, 80, 150)),

            // Period 1 has only room tokens
            OcrTextElement("C003, L002\nWS", OcrRect(105, 105, 195, 145)),
            // Period 2 is empty
        )

        val slots = parser.parseGrid(elements)
        assertTrue(slots.isEmpty())
    }

    @Test
    fun parseGrid_boilerplateOnlyCell_producesNoSlot() {
        // Cells with only "Entire class" or "Batch A" without a course must produce NO slot
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("Wed", OcrRect(10, 100, 80, 150)),
            OcrTextElement("Entire class\nBatch 1", OcrRect(105, 105, 195, 145))
        )

        val slots = parser.parseGrid(elements)
        assertTrue(slots.isEmpty())
    }

    @Test
    fun parseGrid_columnDerivedTime_overridesCellTimeOcr() {
        // If Period 2 is 09:00-09:55, a class occupying Period 2 must ALWAYS get 09:00-09:55
        // even if OCR text inside the cell contains random time-like text
        val elements = listOf(
            OcrTextElement("8:00 - 8:55", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 9:55", OcrRect(200, 30, 300, 60)),
            OcrTextElement("Thu", OcrRect(10, 100, 80, 150)),

            // Cell in Period 2 contains course + misleading time string
            OcrTextElement("ME201\n11:00\nC003", OcrRect(205, 105, 295, 145))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(1, slots.size)
        assertEquals("ME201", slots[0].subjectName)
        assertEquals("09:00", slots[0].startTime)
        assertEquals("09:55", slots[0].endTime)
        assertEquals("C003", slots[0].location)
    }

    @Test
    fun parseGrid_legendMetadataDoesNotGenerateSlots() {
        // Bottom legend entries must enrich cells but never generate timetable slots on their own
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("Fri", OcrRect(10, 100, 80, 150)),
            OcrTextElement("CS203\nWS", OcrRect(105, 105, 195, 145)),

            // Legend at bottom
            OcrTextElement("Courses", OcrRect(50, 250, 200, 270)),
            OcrTextElement("CS203 Computer Science", OcrRect(50, 275, 300, 295)),
            OcrTextElement("Classrooms", OcrRect(50, 300, 200, 320)),
            OcrTextElement("WS Workshop", OcrRect(50, 325, 300, 345))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(1, slots.size)
        assertEquals("CS203", slots[0].subjectName)
        assertEquals("Workshop", slots[0].location)
    }

    @Test
    fun parseGrid_genericDifferentUniversityTimetable_extractsAccurately() {
        // Fully generic test with completely different course codes and rooms (EE, ECE, Robotics Lab)
        val elements = listOf(
            OcrTextElement("08:30 - 09:30", OcrRect(100, 30, 200, 60)),
            OcrTextElement("09:30 - 10:30", OcrRect(200, 30, 300, 60)),
            OcrTextElement("10:30 - 11:30", OcrRect(300, 30, 400, 60)),

            OcrTextElement("Monday", OcrRect(10, 100, 80, 150)),
            OcrTextElement("Tuesday", OcrRect(10, 200, 80, 250)),

            // Monday: EE301 in Period 1, Robotics Lab in Period 2-3 (merged)
            OcrTextElement("EE301\nRoom 401", OcrRect(105, 105, 195, 145)),
            OcrTextElement("ECE305\nRobotics Lab", OcrRect(205, 105, 395, 145)),

            // Tuesday: PHY201 in Period 2 (Period 1 empty)
            OcrTextElement("PHY201\nAuditorium 1", OcrRect(205, 205, 295, 245))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(3, slots.size)

        // Slot 1: Monday EE301 (08:30 - 09:30)
        assertEquals("EE301", slots[0].subjectName)
        assertEquals("Room 401", slots[0].location)
        assertEquals("08:30", slots[0].startTime)
        assertEquals("09:30", slots[0].endTime)
        assertEquals(1, slots[0].dayOfWeek)

        // Slot 2: Monday ECE305 merged (09:30 - 11:30)
        assertEquals("ECE305", slots[1].subjectName)
        assertEquals("Robotics Lab", slots[1].location)
        assertEquals("09:30", slots[1].startTime)
        assertEquals("11:30", slots[1].endTime)
        assertEquals(1, slots[1].dayOfWeek)

        // Slot 3: Tuesday PHY201 (09:30 - 10:30)
        assertEquals("PHY201", slots[2].subjectName)
        assertEquals("Auditorium 1", slots[2].location)
        assertEquals("09:30", slots[2].startTime)
        assertEquals("10:30", slots[2].endTime)
        assertEquals(2, slots[2].dayOfWeek)
    }

    @Test
    fun parseGrid_adjacentSinglePeriodClasses_neverMerge() {
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 10:00", OcrRect(200, 30, 300, 60)),
            OcrTextElement("10:00 - 11:00", OcrRect(300, 30, 400, 60)),

            OcrTextElement("Mon", OcrRect(10, 100, 80, 150)),

            // Period 1 class (ME201)
            OcrTextElement("ME201\nRoom 1", OcrRect(105, 105, 195, 145)),
            // Period 2 class (MA203) immediately following
            OcrTextElement("MA203\nRoom 2", OcrRect(205, 105, 295, 145))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(2, slots.size)
        assertEquals("ME201", slots[0].subjectName)
        assertEquals("08:00", slots[0].startTime)
        assertEquals("09:00", slots[0].endTime)

        assertEquals("MA203", slots[1].subjectName)
        assertEquals("09:00", slots[1].startTime)
        assertEquals("10:00", slots[1].endTime)
    }

    @Test
    fun parseGrid_isolatedClassInLaterPeriod_mapsToCorrectPeriod() {
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 10:00", OcrRect(200, 30, 300, 60)),
            OcrTextElement("10:00 - 11:00", OcrRect(300, 30, 400, 60)),

            OcrTextElement("Thu", OcrRect(10, 100, 80, 150)),

            // Only Period 2 has a class
            OcrTextElement("ME201\nRoom 1", OcrRect(205, 105, 295, 145))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(1, slots.size)
        assertEquals("ME201", slots[0].subjectName)
        assertEquals(4, slots[0].dayOfWeek)
        assertEquals("09:00", slots[0].startTime)
        assertEquals("10:00", slots[0].endTime)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Bug 1 & 2 Regression: False Subject Names / Ambiguous OCR
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun parseGrid_randomNumericOcr_producesNoSlot() {
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("Mon", OcrRect(10, 100, 80, 150)),
            OcrTextElement("42 17", OcrRect(105, 105, 195, 145))
        )

        val slots = parser.parseGrid(elements)
        assertTrue("Numeric-only cell should produce 0 slots", slots.isEmpty())
    }

    @Test
    fun parseGrid_facultyOnlyCell_producesNoSlot() {
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 10:00", OcrRect(200, 30, 300, 60)),
            OcrTextElement("Tue", OcrRect(10, 100, 80, 150)),
            // Period 1: only faculty initials
            OcrTextElement("PKM", OcrRect(105, 105, 195, 145)),
            // Period 2: only faculty initials combo
            OcrTextElement("TN HN", OcrRect(205, 105, 295, 145))
        )

        val slots = parser.parseGrid(elements)
        assertTrue("Faculty-only cells should produce 0 slots", slots.isEmpty())
    }

    @Test
    fun parseGrid_malformedGarbageText_producesNoSlot() {
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("Wed", OcrRect(10, 100, 80, 150)),
            OcrTextElement("Xb4 !@#", OcrRect(105, 105, 195, 145))
        )

        val slots = parser.parseGrid(elements)
        assertTrue("Garbage OCR text should produce 0 slots", slots.isEmpty())
    }

    @Test
    fun parseGrid_ambiguousOcrText_producesZeroClasses() {
        // Regression: ambiguous OCR text that cannot be confidently identified
        // as a course must produce zero classes, not a garbage subject.
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 10:00", OcrRect(200, 30, 300, 60)),
            OcrTextElement("Thu", OcrRect(10, 100, 80, 150)),
            // Period 1: faculty-style initials
            OcrTextElement("SKS/VK", OcrRect(105, 105, 195, 145)),
            // Period 2: isolated short uppercase token (ambiguous)
            OcrTextElement("SwK", OcrRect(205, 105, 295, 145))
        )

        val slots = parser.parseGrid(elements)
        assertTrue(
            "Ambiguous OCR text should produce 0 slots, got: ${slots.map { it.subjectName }}",
            slots.isEmpty()
        )
    }

    @Test
    fun parseGrid_legitimateCourse_accepted() {
        val elements = listOf(
            OcrTextElement("8:00 - 9:00", OcrRect(100, 30, 200, 60)),
            OcrTextElement("Fri", OcrRect(10, 100, 80, 150)),
            OcrTextElement("CS301\nRoom 1", OcrRect(105, 105, 195, 145))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(1, slots.size)
        assertEquals("CS301", slots[0].subjectName)
        assertEquals("Room 1", slots[0].location)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Bug 3 Regression: Merged Cell Start/End Time
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun parseGrid_twoPeriodMergedCell_correctStartEnd() {
        val elements = listOf(
            OcrTextElement("8:00 - 8:55", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 9:55", OcrRect(200, 30, 300, 60)),
            OcrTextElement("10:00 - 10:55", OcrRect(300, 30, 400, 60)),
            OcrTextElement("Mon", OcrRect(10, 100, 80, 150)),
            // Subject spans periods 1-2 (left=105, right=295 → width=190 vs avgCol=100)
            OcrTextElement("CS203\nRoom 5", OcrRect(105, 105, 295, 145))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(1, slots.size)
        assertEquals("CS203", slots[0].subjectName)
        assertEquals("08:00", slots[0].startTime)
        assertEquals("09:55", slots[0].endTime)
    }

    @Test
    fun parseGrid_threePeriodMergedCell_correctStartEnd() {
        val elements = listOf(
            OcrTextElement("8:00 - 8:55", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 9:55", OcrRect(200, 30, 300, 60)),
            OcrTextElement("10:00 - 10:55", OcrRect(300, 30, 400, 60)),
            OcrTextElement("Tue", OcrRect(10, 100, 80, 150)),
            // Subject spans all 3 periods (left=105, right=395 → width=290 vs avgCol=100)
            OcrTextElement("ME201\nWS, L002", OcrRect(105, 105, 395, 145))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(1, slots.size)
        assertEquals("ME201", slots[0].subjectName)
        assertEquals("08:00", slots[0].startTime)
        assertEquals("10:55", slots[0].endTime)
    }

    @Test
    fun parseGrid_mergedCellWithBoundaryNoise_handledCorrectly() {
        // Subject bbox is 92 pixels wide (just under 1 column width of 100).
        // Minor OCR boundary noise must NOT convert this into a merged cell.
        val elements = listOf(
            OcrTextElement("8:00 - 8:55", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 9:55", OcrRect(200, 30, 300, 60)),
            OcrTextElement("10:00 - 10:55", OcrRect(300, 30, 400, 60)),
            OcrTextElement("Wed", OcrRect(10, 100, 80, 150)),
            // Subject in period 2, but bbox extends 3 pixels into period 3 due to OCR noise
            OcrTextElement("MA203", OcrRect(202, 105, 303, 145))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(1, slots.size)
        assertEquals("MA203", slots[0].subjectName)
        // Must stay single-period (period 2), NOT merge with period 3
        assertEquals("09:00", slots[0].startTime)
        assertEquals("09:55", slots[0].endTime)
    }

    @Test
    fun parseGrid_emptyPrecedingAndFollowingPeriods_noFalseExpansion() {
        // Class in period 3, periods 1-2 and 4 are empty.
        // Must not merge with empty adjacent periods.
        val elements = listOf(
            OcrTextElement("8:00 - 8:55", OcrRect(100, 30, 200, 60)),
            OcrTextElement("9:00 - 9:55", OcrRect(200, 30, 300, 60)),
            OcrTextElement("10:00 - 10:55", OcrRect(300, 30, 400, 60)),
            OcrTextElement("11:00 - 11:55", OcrRect(400, 30, 500, 60)),
            OcrTextElement("Thu", OcrRect(10, 100, 80, 150)),
            OcrTextElement("EE301\nLab 1", OcrRect(305, 105, 395, 145))
        )

        val slots = parser.parseGrid(elements)
        assertEquals(1, slots.size)
        assertEquals("EE301", slots[0].subjectName)
        assertEquals("10:00", slots[0].startTime)
        assertEquals("10:55", slots[0].endTime)
    }
}
