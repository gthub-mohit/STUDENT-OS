package com.studentos.feature.attendance.domain.repository

import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot

/**
 * TimetableRepository — Domain repository contract for timetable management and transactional import.
 */
interface TimetableRepository {
    suspend fun importTimetable(
        slots: List<ParsedTimetableSlot>,
        replaceExisting: Boolean,
        horizonDays: Int
    ): AppResult<Unit>
}
