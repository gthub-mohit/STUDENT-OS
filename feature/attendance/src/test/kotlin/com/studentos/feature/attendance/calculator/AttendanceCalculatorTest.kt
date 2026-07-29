package com.studentos.feature.attendance.calculator

import com.studentos.feature.attendance.domain.calculator.AttendanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * AttendanceCalculatorTest — Unit tests verifying AttendanceCalculator for 10 known input combinations.
 */
class AttendanceCalculatorTest {

    @Test
    fun test1_perfectAttendance() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 10,
            absent = 0,
            cancelled = 0,
            holiday = 0,
            extraPresent = 0
        )
        assertEquals(100.0, result, 0.0001)
    }

    @Test
    fun test2_partialAttendance() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 7,
            absent = 3,
            cancelled = 0,
            holiday = 0,
            extraPresent = 0
        )
        assertEquals(70.0, result, 0.0001)
    }

    @Test
    fun test3_cancelledClassesExcludedFromDenominator() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 8,
            absent = 2,
            cancelled = 5,
            holiday = 0,
            extraPresent = 0
        )
        assertEquals(80.0, result, 0.0001)
    }

    @Test
    fun test4_holidaysExcludedFromDenominator() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 6,
            absent = 4,
            cancelled = 0,
            holiday = 2,
            extraPresent = 0
        )
        assertEquals(60.0, result, 0.0001)
    }

    @Test
    fun test5_extraClassesIncluded() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 5,
            absent = 5,
            cancelled = 0,
            holiday = 0,
            extraPresent = 2
        )
        assertEquals(58.333333333333336, result, 0.0001)
    }

    @Test
    fun test6_allCancelledClasses() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 0,
            absent = 0,
            cancelled = 10,
            holiday = 0,
            extraPresent = 0
        )
        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun test7_zeroClassesHeld() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 0,
            absent = 0,
            cancelled = 0,
            holiday = 0,
            extraPresent = 0
        )
        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun test8_hundredPercentViaExtraClasses() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 0,
            absent = 0,
            cancelled = 0,
            holiday = 0,
            extraPresent = 5
        )
        assertEquals(100.0, result, 0.0001)
    }

    @Test
    fun test9_boundaryThresholdExact() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 75,
            absent = 25,
            cancelled = 0,
            holiday = 0,
            extraPresent = 0
        )
        assertEquals(75.0, result, 0.0001)
    }

    @Test
    fun test10_lowAttendanceBoundary() {
        val result = AttendanceCalculator.calculatePercentage(
            present = 1,
            absent = 99,
            cancelled = 0,
            holiday = 0,
            extraPresent = 0
        )
        assertEquals(1.0, result, 0.0001)
    }
}
