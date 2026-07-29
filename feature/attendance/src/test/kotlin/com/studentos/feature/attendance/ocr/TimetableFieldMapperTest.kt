package com.studentos.feature.attendance.ocr

import com.studentos.feature.attendance.data.ocr.TimetableFieldMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TimetableFieldMapperTest — Unit tests for TimetableFieldMapper testing 5 distinct timetable layouts.
 */
class TimetableFieldMapperTest {

    private lateinit var mapper: TimetableFieldMapper

    @Before
    fun setUp() {
        mapper = TimetableFieldMapper()
    }

    @Test
    fun layout1_standardGridWithDayHeaders() {
        val rawText = """
            Monday
            09:00 - 10:00 Data Structures - Room 101
            10:00 - 11:00 Algorithms - Lab 2
            Tuesday
            11:00 - 12:00 Software Engineering
        """.trimIndent()

        val result = mapper.mapFromRawText(rawText, defaultConfidence = 0.90f)

        assertEquals(3, result.slots.size)
        assertFalse(result.hasWarnings)

        val slot1 = result.slots[0]
        assertEquals(1, slot1.dayOfWeek) // Monday
        assertEquals("09:00", slot1.startTime)
        assertEquals("10:00", slot1.endTime)
        assertEquals("Data Structures", slot1.subjectName)
        assertEquals("Room 101", slot1.location)
        assertFalse(slot1.isLowConfidence)

        val slot3 = result.slots[2]
        assertEquals(2, slot3.dayOfWeek) // Tuesday
        assertEquals("11:00", slot3.startTime)
        assertEquals("12:00", slot3.endTime)
        assertEquals("Software Engineering", slot3.subjectName)
    }

    @Test
    fun layout2_abbreviatedDayNames() {
        val rawText = """
            MON
            08:30 - 09:30 Operating Systems
            WED
            14:00 - 15:30 Computer Networks - Room 404
        """.trimIndent()

        val result = mapper.mapFromRawText(rawText, defaultConfidence = 0.88f)

        assertEquals(2, result.slots.size)

        assertEquals(1, result.slots[0].dayOfWeek) // Monday
        assertEquals("Operating Systems", result.slots[0].subjectName)

        assertEquals(3, result.slots[1].dayOfWeek) // Wednesday
        assertEquals("14:00", result.slots[1].startTime)
        assertEquals("15:30", result.slots[1].endTime)
        assertEquals("Computer Networks", result.slots[1].subjectName)
        assertEquals("Room 404", result.slots[1].location)
    }

    @Test
    fun layout3_twelveHourAmPmFormat() {
        val rawText = """
            Thursday
            9:00 AM - 10:30 AM Calculus I
            2:00 PM - 3:30 PM Physics II - Lab B
        """.trimIndent()

        val result = mapper.mapFromRawText(rawText, defaultConfidence = 0.92f)

        assertEquals(2, result.slots.size)

        val slot1 = result.slots[0]
        assertEquals(4, slot1.dayOfWeek) // Thursday
        assertEquals("09:00", slot1.startTime)
        assertEquals("10:30", slot1.endTime)
        assertEquals("Calculus I", slot1.subjectName)

        val slot2 = result.slots[1]
        assertEquals("14:00", slot2.startTime)
        assertEquals("15:30", slot2.endTime)
        assertEquals("Physics II", slot2.subjectName)
        assertEquals("Lab B", slot2.location)
    }

    @Test
    fun layout4_lowConfidenceFlagging() {
        val rawText = """
            Friday
            10:00 - 11:00 Machine Learning - Hall A
        """.trimIndent()

        // Passing confidence below threshold (0.75f < 0.80f)
        val result = mapper.mapFromRawText(rawText, defaultConfidence = 0.75f)

        assertEquals(1, result.slots.size)
        assertTrue(result.hasWarnings)

        val slot = result.slots[0]
        assertEquals(5, slot.dayOfWeek) // Friday
        assertTrue(slot.isLowConfidence)
    }

    @Test
    fun layout5_emptyOrNoisyTextReturnsWarnings() {
        val rawText = "No time information detected in this random image."

        val result = mapper.mapFromRawText(rawText, defaultConfidence = 0.90f)

        assertTrue(result.slots.isEmpty())
        assertTrue(result.hasWarnings)
    }
}
