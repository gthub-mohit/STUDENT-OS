package com.studentos.feature.attendance.domain.repository

import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import kotlinx.coroutines.flow.Flow

/**
 * TimetableRepository — Domain repository contract for timetable management and transactional import.
 */
interface TimetableRepository {
    fun getAllSlots(): Flow<List<TimetableSlotEntity>>
    suspend fun importTimetable(
        slots: List<ParsedTimetableSlot>,
        replaceExisting: Boolean,
        horizonDays: Int = 90
    ): AppResult<Unit>
    suspend fun addSlot(
        slot: TimetableSlotEntity,
        horizonDays: Int = 90
    ): AppResult<Long>
    suspend fun updateSlot(
        slot: TimetableSlotEntity,
        horizonDays: Int = 90
    ): AppResult<Unit>
    suspend fun deleteSlot(
        slotId: Long
    ): AppResult<Unit>
}
