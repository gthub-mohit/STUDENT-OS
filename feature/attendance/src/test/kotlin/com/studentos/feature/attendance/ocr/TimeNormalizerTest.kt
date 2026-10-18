package com.studentos.feature.attendance.ocr

import com.studentos.feature.attendance.data.ocr.TimeNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TimeNormalizerTest {

    @Test
    fun parseTimeRange_colonFormats_parsedCorrectly() {
        val range1 = TimeNormalizer.parseTimeRange("09:00 - 10:00")
        assertNotNull(range1)
        assertEquals("09:00" to "10:00", range1)

        val range2 = TimeNormalizer.parseTimeRange("8:00 - 8:55")
        assertNotNull(range2)
        assertEquals("08:00" to "08:55", range2)

        val range3 = TimeNormalizer.parseTimeRange("14:00 - 15:30")
        assertNotNull(range3)
        assertEquals("14:00" to "15:30", range3)
    }

    @Test
    fun parseTimeRange_dotFormats_parsedCorrectly() {
        val range1 = TimeNormalizer.parseTimeRange("9.30 - 10.30")
        assertNotNull(range1)
        assertEquals("09:30" to "10:30", range1)

        val range2 = TimeNormalizer.parseTimeRange("8.00 - 8.55")
        assertNotNull(range2)
        assertEquals("08:00" to "08:55", range2)

        val range3 = TimeNormalizer.parseTimeRange("09.30 - 11.00")
        assertNotNull(range3)
        assertEquals("09:30" to "11:00", range3)
    }

    @Test
    fun parseTimeRange_amPmFormats_parsedCorrectly() {
        // 9-10 AM (start inherits AM)
        val range1 = TimeNormalizer.parseTimeRange("9-10 AM")
        assertNotNull(range1)
        assertEquals("09:00" to "10:00", range1)

        // 9 AM - 10 AM
        val range2 = TimeNormalizer.parseTimeRange("9 AM - 10 AM")
        assertNotNull(range2)
        assertEquals("09:00" to "10:00", range2)

        // 9:30 AM - 10:30 AM
        val range3 = TimeNormalizer.parseTimeRange("9:30 AM - 10:30 AM")
        assertNotNull(range3)
        assertEquals("09:30" to "10:30", range3)

        // 9.30 AM - 10.30 AM
        val range4 = TimeNormalizer.parseTimeRange("9.30 AM - 10.30 AM")
        assertNotNull(range4)
        assertEquals("09:30" to "10:30", range4)

        // 1 - 2 PM (both afternoon)
        val range5 = TimeNormalizer.parseTimeRange("1 - 2 PM")
        assertNotNull(range5)
        assertEquals("13:00" to "14:00", range5)

        // 2:00 PM - 3:30 PM
        val range6 = TimeNormalizer.parseTimeRange("2:00 PM - 3:30 PM")
        assertNotNull(range6)
        assertEquals("14:00" to "15:30", range6)

        // 11 AM - 1 PM (cross-noon)
        val range7 = TimeNormalizer.parseTimeRange("11 AM - 1 PM")
        assertNotNull(range7)
        assertEquals("11:00" to "13:00", range7)

        // 11:00 AM - 1:00 PM
        val range8 = TimeNormalizer.parseTimeRange("11:00 AM - 1:00 PM")
        assertNotNull(range8)
        assertEquals("11:00" to "13:00", range8)
    }

    @Test
    fun parseTimeRange_enDashAndTo_parsedCorrectly() {
        val rangeEn = TimeNormalizer.parseTimeRange("09:00 – 10:00")
        assertNotNull(rangeEn)
        assertEquals("09:00" to "10:00", rangeEn)

        val rangeTo = TimeNormalizer.parseTimeRange("9:00 to 10:00")
        assertNotNull(rangeTo)
        assertEquals("09:00" to "10:00", rangeTo)
    }

    @Test
    fun parseTimeRange_malformedOrReversed_returnsNull() {
        // Start time after end time
        assertNull(TimeNormalizer.parseTimeRange("10:00 - 09:00"))
        // Same time
        assertNull(TimeNormalizer.parseTimeRange("09:00 - 09:00"))
        // Invalid minutes
        assertNull(TimeNormalizer.parseTimeRange("09:65 - 10:00"))
        // Random text
        assertNull(TimeNormalizer.parseTimeRange("Mathematics Room 101"))
    }

    @Test
    fun normalizeSingleTime_validInputs() {
        assertEquals("09:30", TimeNormalizer.normalizeSingleTime("9.30"))
        assertEquals("09:30", TimeNormalizer.normalizeSingleTime("9:30"))
        assertEquals("09:00", TimeNormalizer.normalizeSingleTime("9 AM"))
        assertEquals("14:00", TimeNormalizer.normalizeSingleTime("2 PM"))
        assertEquals("14:30", TimeNormalizer.normalizeSingleTime("2.30 PM"))
        assertEquals("12:00", TimeNormalizer.normalizeSingleTime("12 PM"))
        assertEquals("00:00", TimeNormalizer.normalizeSingleTime("12 AM"))
        assertEquals("15:00", TimeNormalizer.normalizeSingleTime("15:00"))
    }

    @Test
    fun parsePeriodHeader_validPatterns() {
        assertEquals(1, TimeNormalizer.parsePeriodHeader("Period 1"))
        assertEquals(2, TimeNormalizer.parsePeriodHeader("PERIOD 2"))
        assertEquals(1, TimeNormalizer.parsePeriodHeader("P1"))
        assertEquals(3, TimeNormalizer.parsePeriodHeader("P-3"))
        assertEquals(4, TimeNormalizer.parsePeriodHeader("Slot 4"))
        assertEquals(1, TimeNormalizer.parsePeriodHeader("1st"))
        assertEquals(5, TimeNormalizer.parsePeriodHeader("5"))
        assertEquals(10, TimeNormalizer.parsePeriodHeader("10"))
        assertNull(TimeNormalizer.parsePeriodHeader("13"))
        assertNull(TimeNormalizer.parsePeriodHeader("Entire Class"))
    }

    @Test
    fun parseDayOfWeek_resilientToNoise() {
        assertEquals(1, TimeNormalizer.parseDayOfWeek("Monday"))
        assertEquals(1, TimeNormalizer.parseDayOfWeek("Mon."))
        assertEquals(1, TimeNormalizer.parseDayOfWeek("1. MON"))
        assertEquals(2, TimeNormalizer.parseDayOfWeek("Tue"))
        assertEquals(2, TimeNormalizer.parseDayOfWeek("Tues."))
        assertEquals(2, TimeNormalizer.parseDayOfWeek("Day: Tuesday"))
        assertEquals(3, TimeNormalizer.parseDayOfWeek("Wed"))
        assertEquals(4, TimeNormalizer.parseDayOfWeek("Thursday"))
        assertEquals(5, TimeNormalizer.parseDayOfWeek("Fri"))
        assertEquals(6, TimeNormalizer.parseDayOfWeek("Sat"))
        assertEquals(7, TimeNormalizer.parseDayOfWeek("Sun"))
        assertNull(TimeNormalizer.parseDayOfWeek("Mathematics"))
    }
}
