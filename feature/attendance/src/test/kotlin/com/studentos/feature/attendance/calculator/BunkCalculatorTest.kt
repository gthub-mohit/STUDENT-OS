package com.studentos.feature.attendance.calculator

import com.studentos.feature.attendance.domain.calculator.AttendanceCalculator
import com.studentos.feature.attendance.domain.calculator.BunkCalculator
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.forAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BunkCalculatorTest — Property-based and deterministic unit test suite for BunkCalculator.
 */
class BunkCalculatorTest {

    private val eps = 1e-9

    @Test
    fun propertyBased_testBunkCalculatorInvariants() {
        runBlocking {
            forAll(
                Arb.int(0..200), // present
                Arb.int(0..200), // absent
                Arb.int(0..50),  // cancelled
                Arb.int(0..50),  // holiday
                Arb.int(0..50),  // extraPresent
                Arb.int(1..99)   // threshold
            ) { present, absent, cancelled, holiday, extraPresent, threshold ->
                val h = present + absent + extraPresent

                if (h <= 0) return@forAll true

                val currentPct = AttendanceCalculator.calculatePercentage(present, absent, cancelled, holiday, extraPresent)
                val canSkip = BunkCalculator.canSkip(present, absent, cancelled, holiday, extraPresent, threshold)
                val mustAttend = BunkCalculator.mustAttend(present, absent, cancelled, holiday, extraPresent, threshold)

                // 1. Mutual Exclusivity
                if (currentPct >= threshold - eps) {
                    if (mustAttend != 0) return@forAll false
                } else {
                    if (canSkip != 0) return@forAll false
                }

                // 2. Skip Boundary Invariant
                if (canSkip > 0 && canSkip < Int.MAX_VALUE) {
                    val safePct = AttendanceCalculator.calculatePercentage(present, absent + canSkip, cancelled, holiday, extraPresent)
                    val unsafePct = AttendanceCalculator.calculatePercentage(present, absent + canSkip + 1, cancelled, holiday, extraPresent)
                    if (safePct < threshold - eps || unsafePct >= threshold + eps) return@forAll false
                }

                // 3. Attend Boundary Invariant
                if (mustAttend > 0 && mustAttend < Int.MAX_VALUE && threshold < 100) {
                    val safePct = AttendanceCalculator.calculatePercentage(present + mustAttend, absent, cancelled, holiday, extraPresent)
                    val unsafePct = AttendanceCalculator.calculatePercentage(present + mustAttend - 1, absent, cancelled, holiday, extraPresent)
                    if (safePct < threshold - eps || unsafePct >= threshold + eps) return@forAll false
                }

                true
            }
        }
    }

    // ── Deterministic Unit Tests ───────────────────────────────────────────────

    @Test
    fun test100PercentAttendance() {
        val canSkip = BunkCalculator.canSkip(10, 0, 0, 0, 0, 75)
        val mustAttend = BunkCalculator.mustAttend(10, 0, 0, 0, 0, 75)
        assertEquals(3, canSkip)
        assertEquals(0, mustAttend)
    }

    @Test
    fun testExactlyThreshold75Percent() {
        val canSkip = BunkCalculator.canSkip(3, 1, 0, 0, 0, 75)
        val mustAttend = BunkCalculator.mustAttend(3, 1, 0, 0, 0, 75)
        assertEquals(0, canSkip)
        assertEquals(0, mustAttend)
    }

    @Test
    fun testBelowThreshold() {
        val canSkip = BunkCalculator.canSkip(2, 2, 0, 0, 0, 75)
        val mustAttend = BunkCalculator.mustAttend(2, 2, 0, 0, 0, 75)
        assertEquals(0, canSkip)
        assertEquals(4, mustAttend)
    }

    @Test
    fun testZeroHeldClasses() {
        val canSkip = BunkCalculator.canSkip(0, 0, 0, 0, 0, 75)
        val mustAttend = BunkCalculator.mustAttend(0, 0, 0, 0, 0, 75)
        assertEquals(0, canSkip)
        assertEquals(0, mustAttend)
    }

    @Test
    fun testAllCancelledClasses() {
        val canSkip = BunkCalculator.canSkip(0, 0, 10, 5, 0, 75)
        val mustAttend = BunkCalculator.mustAttend(0, 0, 10, 5, 0, 75)
        assertEquals(0, canSkip)
        assertEquals(0, mustAttend)
    }

    @Test
    fun testExtraClasses() {
        val canSkip = BunkCalculator.canSkip(5, 5, 0, 0, 5, 75)
        val mustAttend = BunkCalculator.mustAttend(5, 5, 0, 0, 5, 75)
        assertEquals(0, canSkip)
        assertEquals(5, mustAttend)
    }

    @Test
    fun testThreshold0() {
        val canSkip = BunkCalculator.canSkip(5, 5, 0, 0, 0, 0)
        val mustAttend = BunkCalculator.mustAttend(5, 5, 0, 0, 0, 0)
        assertEquals(Int.MAX_VALUE, canSkip)
        assertEquals(0, mustAttend)
    }

    @Test
    fun testThreshold100() {
        val canSkip = BunkCalculator.canSkip(9, 1, 0, 0, 0, 100)
        val mustAttend = BunkCalculator.mustAttend(9, 1, 0, 0, 0, 100)
        assertEquals(0, canSkip)
        assertEquals(Int.MAX_VALUE, mustAttend)
    }
}
