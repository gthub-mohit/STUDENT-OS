package com.studentos.feature.attendance.domain.usecase

import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import com.studentos.feature.attendance.domain.repository.TimetableRepository
import javax.inject.Inject

/**
 * ImportTimetableUseCase — Domain use case for importing extracted timetable slots.
 *
 * Enforces the Generation Horizon Guard: maximum horizon is hard-clamped to 365 days.
 */
class ImportTimetableUseCase @Inject constructor(
    private val timetableRepository: TimetableRepository
) {

    suspend operator fun invoke(
        slots: List<ParsedTimetableSlot>,
        replaceExisting: Boolean = false,
        horizonDays: Int = 90
    ): AppResult<Unit> {
        val clampedHorizon = minOf(horizonDays, MAX_HORIZON_DAYS)
        return timetableRepository.importTimetable(
            slots = slots,
            replaceExisting = replaceExisting,
            horizonDays = clampedHorizon
        )
    }

    companion object {
        const val MAX_HORIZON_DAYS = 365
    }
}
