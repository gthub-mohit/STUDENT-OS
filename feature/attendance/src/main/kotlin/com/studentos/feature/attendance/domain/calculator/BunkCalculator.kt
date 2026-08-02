package com.studentos.feature.attendance.domain.calculator

import kotlin.math.ceil
import kotlin.math.floor

/**
 * BunkCalculator — Pure Kotlin domain calculator determining safe skippable classes, required attendance,
 * and future attendance predictions.
 *
 * Formula definitions (design.md Line 297 & ui-blueprint.md Line 931):
 * P = present + extraPresent
 * H = present + absent + extraPresent
 *
 * canSkip = max(0, floor((100.0 * P - threshold * H) / threshold))
 * mustAttend = max(0, ceil((threshold * H - 100.0 * P) / (100.0 - threshold)))
 */
object BunkCalculator {

    fun canSkip(
        present: Int,
        absent: Int,
        cancelled: Int,
        holiday: Int,
        extraPresent: Int,
        threshold: Int
    ): Int {
        val p = (present + extraPresent).coerceAtLeast(0).toDouble()
        val h = (present + absent + extraPresent).coerceAtLeast(0).toDouble()

        if (h <= 0.0) return 0
        if (threshold <= 0) return Int.MAX_VALUE

        val currentPct = (p / h) * 100.0
        val t = threshold.toDouble()
        if (currentPct < t) return 0

        val canSkipVal = floor((100.0 * p - t * h) / t)
        return canSkipVal.toInt().coerceAtLeast(0)
    }

    fun mustAttend(
        present: Int,
        absent: Int,
        cancelled: Int,
        holiday: Int,
        extraPresent: Int,
        threshold: Int
    ): Int {
        val p = (present + extraPresent).coerceAtLeast(0).toDouble()
        val h = (present + absent + extraPresent).coerceAtLeast(0).toDouble()

        if (h <= 0.0) return 0

        val currentPct = (p / h) * 100.0
        val t = threshold.toDouble()
        if (currentPct >= t) return 0

        if (threshold >= 100) return Int.MAX_VALUE

        val mustAttendVal = ceil((t * h - 100.0 * p) / (100.0 - t))
        return mustAttendVal.toInt().coerceAtLeast(0)
    }

    fun predictAttendance(
        present: Int,
        absent: Int,
        cancelled: Int,
        holiday: Int,
        extraPresent: Int,
        futureAttended: Int,
        futureBunked: Int
    ): Double {
        return AttendanceCalculator.calculatePercentage(
            present = present + futureAttended.coerceAtLeast(0),
            absent = absent + futureBunked.coerceAtLeast(0),
            cancelled = cancelled,
            holiday = holiday,
            extraPresent = extraPresent
        )
    }
}
