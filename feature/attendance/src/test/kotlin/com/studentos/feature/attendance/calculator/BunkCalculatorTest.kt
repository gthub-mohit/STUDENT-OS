package com.studentos.feature.attendance.calculator

import com.studentos.feature.attendance.domain.calculator.AttendanceCalculator
import com.studentos.feature.attendance.domain.calculator.BunkCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BunkCalculatorTest — Deterministic unit test suite for BunkCalculator and prediction engine.
 */
class BunkCalculatorTest {

    private val eps = 1e-9

    @Test
    fun testBunkCalculatorInvariants() {
        for (present in listOf(0, 10, 50, 100)) {
            for (absent in listOf(0, 5, 20, 50)) {
                for (extraPresent in listOf(0, 2, 10)) {
                    for (threshold in listOf(75, 80, 90)) {
                        val h = present + absent + extraPresent
                        if (h <= 0) continue

                        val currentPct = AttendanceCalculator.calculatePercentage(present, absent, 0, 0, extraPresent)
                        val canSkip = BunkCalculator.canSkip(present, absent, 0, 0, extraPresent, threshold)
                        val mustAttend = BunkCalculator.mustAttend(present, absent, 0, 0, extraPresent, threshold)

                        if (currentPct >= (threshold.toDouble() - eps)) {
                            assertEquals(0, mustAttend)
                        } else {
                            assertEquals(0, canSkip)
                        }

                        if (canSkip > 0 && canSkip < Int.MAX_VALUE) {
                            val safePct = AttendanceCalculator.calculatePercentage(present, absent + canSkip, 0, 0, extraPresent)
                            assertTrue(safePct >= (threshold.toDouble() - eps))
                        }

                        if (mustAttend > 0 && mustAttend < Int.MAX_VALUE && threshold < 100) {
                            val safePct = AttendanceCalculator.calculatePercentage(present + mustAttend, absent, 0, 0, extraPresent)
                            assertTrue(safePct >= (threshold.toDouble() - eps))
                        }
                    }
                }
            }
        }
    }

    @Test
    fun calculateCanSkip_returnsCorrectValue() {
        val canSkip = BunkCalculator.canSkip(
            present = 80,
            absent = 20,
            cancelled = 0,
            holiday = 0,
            extraPresent = 0,
            threshold = 75
        )
        assertEquals(6, canSkip)
    }

    @Test
    fun calculateMustAttend_returnsCorrectValue() {
        val mustAttend = BunkCalculator.mustAttend(
            present = 60,
            absent = 40,
            cancelled = 0,
            holiday = 0,
            extraPresent = 0,
            threshold = 75
        )
        assertEquals(60, mustAttend)
    }

    @Test
    fun predictAttendance_calculatesFuturePercentageCorrectly() {
        // Present = 70, Absent = 30 -> Current = 70%
        // Predict attending 10 future classes -> (70 + 10) / (100 + 10) = 80 / 110 = 72.727%
        val predictedAttendedPct = BunkCalculator.predictAttendance(
            present = 70,
            absent = 30,
            cancelled = 0,
            holiday = 0,
            extraPresent = 0,
            futureAttended = 10,
            futureBunked = 0
        )
        assertEquals(72.727, predictedAttendedPct, 0.01)

        // Predict bunking 10 future classes -> 70 / (100 + 10) = 70 / 110 = 63.636%
        val predictedBunkedPct = BunkCalculator.predictAttendance(
            present = 70,
            absent = 30,
            cancelled = 0,
            holiday = 0,
            extraPresent = 0,
            futureAttended = 0,
            futureBunked = 10
        )
        assertEquals(63.636, predictedBunkedPct, 0.01)
    }

    @Test
    fun subjectSpecific_canSkipAndMustAttendCalculations() {
        // ME201: 14 attended out of 17 held (82.35%), threshold 75% -> can skip floor((100*14 - 75*17)/75) = floor(125/75) = 1
        val me201CanSkip = BunkCalculator.canSkip(
            present = 14,
            absent = 3,
            cancelled = 2,
            holiday = 0,
            extraPresent = 0,
            threshold = 75
        )
        assertEquals(1, me201CanSkip)

        // CS203: 11 attended out of 16 held (68.75%), threshold 75% -> must attend ceil((75*16 - 100*11)/(100-75)) = ceil(100/25) = 4
        val cs203MustAttend = BunkCalculator.mustAttend(
            present = 11,
            absent = 5,
            cancelled = 1,
            holiday = 0,
            extraPresent = 0,
            threshold = 75
        )
        assertEquals(4, cs203MustAttend)

        // VAC202: 0 attended out of 0 held
        val vacCanSkip = BunkCalculator.canSkip(0, 0, 0, 0, 0, 75)
        val vacMustAttend = BunkCalculator.mustAttend(0, 0, 0, 0, 0, 75)
        assertEquals(0, vacCanSkip)
        assertEquals(0, vacMustAttend)
    }
}
