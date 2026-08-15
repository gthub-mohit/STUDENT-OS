package com.studentos.feature.attendance.ocr

import com.studentos.feature.attendance.data.ocr.GridTimetableParser
import com.studentos.feature.attendance.data.ocr.TimetableFieldMapper
import com.studentos.feature.attendance.data.ocr.TimetableValidator
import com.studentos.feature.attendance.data.ocr.model.OcrRect
import com.studentos.feature.attendance.data.ocr.model.OcrTextElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableFieldMapperTest {

    private val validator = TimetableValidator()
    private val gridParser = GridTimetableParser(validator)
    private val mapper = TimetableFieldMapper(gridParser, validator)

    @Test
    fun mapFromRawText_emptyText_returnsEmptyListWithWarnings() {
        val result = mapper.mapFromRawText("")
        assertTrue(result.slots.isEmpty())
        assertTrue(result.hasWarnings)
    }

    @Test
    fun mapFromRawText_valid24HourTimetable_parsesSlotsCorrectly() {
        val rawText = """
            Monday
            09:00 - 10:00 Mathematics - Room 101
            10:00 - 11:00 Computer Science - Lab 2
            
            Wednesday
            14:00 - 15:00 Physics (Lecture Hall 1)
        """.trimIndent()

        val result = mapper.mapFromRawText(rawText, defaultConfidence = 0.90f)

        assertFalse(result.hasWarnings)
        assertEquals(3, result.slots.size)

        // Slot 1
        assertEquals(1, result.slots[0].dayOfWeek)
        assertEquals("09:00", result.slots[0].startTime)
        assertEquals("10:00", result.slots[0].endTime)
        assertEquals("Mathematics", result.slots[0].subjectName)
        assertEquals("Room 101", result.slots[0].location)
        assertFalse(result.slots[0].isLowConfidence)

        // Slot 2
        assertEquals(1, result.slots[1].dayOfWeek)
        assertEquals("10:00", result.slots[1].startTime)
        assertEquals("11:00", result.slots[1].endTime)
        assertEquals("Computer Science", result.slots[1].subjectName)
        assertEquals("Lab 2", result.slots[1].location)

        // Slot 3
        assertEquals(3, result.slots[2].dayOfWeek)
        assertEquals("14:00", result.slots[2].startTime)
        assertEquals("15:00", result.slots[2].endTime)
        assertEquals("Physics", result.slots[2].subjectName)
        assertEquals("Lecture Hall 1", result.slots[2].location)
    }

    @Test
    fun mapFromRawText_valid12HourAmPm_normalizesTo24HourTime() {
        val rawText = """
            Friday
            9:00 AM - 10:00 AM Chemistry - Room A
            2:00 PM - 3:30 PM Data Structures
        """.trimIndent()

        val result = mapper.mapFromRawText(rawText, defaultConfidence = 0.85f)

        assertEquals(2, result.slots.size)

        assertEquals("09:00", result.slots[0].startTime)
        assertEquals("10:00", result.slots[0].endTime)

        assertEquals("14:00", result.slots[1].startTime)
        assertEquals("15:30", result.slots[1].endTime)
        assertNull(result.slots[1].location)
    }

    @Test
    fun mapFromRawText_lowConfidence_flagsLowConfidenceAndWarnings() {
        val rawText = """
            Tuesday
            09:00 - 10:00 Biology
        """.trimIndent()

        // Pass 0.70f confidence (below 0.80f threshold)
        val result = mapper.mapFromRawText(rawText, defaultConfidence = 0.70f)

        assertEquals(1, result.slots.size)
        assertTrue(result.slots[0].isLowConfidence)
        assertTrue(result.hasWarnings)
    }

    @Test
    fun mapFromElements_gridElements_invokesGridParser() {
        val elements = listOf(
            OcrTextElement("1", OcrRect(100, 20, 150, 40)),
            OcrTextElement("09:00 - 10:00", OcrRect(100, 45, 200, 75)),
            OcrTextElement("Monday", OcrRect(10, 100, 80, 150)),
            OcrTextElement("Tuesday", OcrRect(10, 200, 80, 250)),
            OcrTextElement("MATH101\nRoom 1", OcrRect(105, 105, 195, 185))
        )

        val result = mapper.mapFromElements(elements)
        assertEquals(1, result.slots.size)
        assertEquals("MATH101", result.slots[0].subjectName)
        assertEquals("09:00", result.slots[0].startTime)
        assertEquals("10:00", result.slots[0].endTime)
    }

    @Test
    fun validator_rejectsInvalidTimesAndSubjects() {
        // "13:00" as subject or invalid minutes
        val invalidSlots = mapper.mapFromRawText(
            """
                Monday
                13:00 - 13:55 13:00
            """.trimIndent()
        )
        // 13:00 should not be accepted as a subject
        assertTrue(invalidSlots.slots.isEmpty())
    }
}
