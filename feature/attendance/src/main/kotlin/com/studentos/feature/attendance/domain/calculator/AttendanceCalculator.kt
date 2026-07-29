package com.studentos.feature.attendance.domain.calculator

/**
 * AttendanceCalculator — Pure Kotlin domain calculator for subject attendance percentages.
 *
 * Formula (design.md Line 290):
 * percentage = (present + extraPresent) / (total - cancelled - holiday) * 100.0
 * where total = present + absent + cancelled + holiday + extraPresent.
 */
object AttendanceCalculator {

    fun calculatePercentage(
        present: Int,
        absent: Int,
        cancelled: Int,
        holiday: Int,
        extraPresent: Int
    ): Double {
        val total = present + absent + cancelled + holiday + extraPresent
        val denominator = total - cancelled - holiday
        if (denominator <= 0) return 0.0
        return (present + extraPresent).toDouble() / denominator * 100.0
    }
}
