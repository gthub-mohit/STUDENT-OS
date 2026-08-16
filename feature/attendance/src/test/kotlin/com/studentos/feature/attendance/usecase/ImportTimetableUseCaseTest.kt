package com.studentos.feature.attendance.usecase

import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import com.studentos.feature.attendance.domain.repository.TimetableRepository
import com.studentos.feature.attendance.domain.usecase.ImportTimetableUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ImportTimetableUseCaseTest — Unit tests for ImportTimetableUseCase.
 */
class ImportTimetableUseCaseTest {

    private class FakeTimetableRepository : TimetableRepository {
        var lastImportedSlots: List<ParsedTimetableSlot>? = null
        var lastReplaceExisting: Boolean? = null
        var lastHorizonDays: Int? = null

        override fun getAllSlots(): kotlinx.coroutines.flow.Flow<List<com.studentos.core.database.entity.TimetableSlotEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())

        override suspend fun importTimetable(
            slots: List<ParsedTimetableSlot>,
            replaceExisting: Boolean,
            horizonDays: Int
        ): AppResult<Unit> {
            lastImportedSlots = slots
            lastReplaceExisting = replaceExisting
            lastHorizonDays = horizonDays
            return AppResult.Success(Unit)
        }

        override suspend fun addSlot(slot: com.studentos.core.database.entity.TimetableSlotEntity, horizonDays: Int): AppResult<Long> = AppResult.Success(1L)
        override suspend fun updateSlot(slot: com.studentos.core.database.entity.TimetableSlotEntity, horizonDays: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteSlot(slotId: Long): AppResult<Unit> = AppResult.Success(Unit)
    }

    @Test
    fun horizonDaysExceeding365_isClampedTo365() = runBlocking {
        val fakeRepo = FakeTimetableRepository()
        val useCase = ImportTimetableUseCase(fakeRepo)

        val slots = listOf(
            ParsedTimetableSlot(
                dayOfWeek = 1,
                startTime = "09:00",
                endTime = "10:00",
                subjectName = "Operating Systems"
            )
        )

        // Passing 500 days horizon
        val result = useCase(slots = slots, replaceExisting = false, horizonDays = 500)

        assertTrue(result is AppResult.Success)
        assertEquals(365, fakeRepo.lastHorizonDays)
    }

    @Test
    fun horizonDaysBelow365_isPreserved() = runBlocking {
        val fakeRepo = FakeTimetableRepository()
        val useCase = ImportTimetableUseCase(fakeRepo)

        val slots = listOf(
            ParsedTimetableSlot(
                dayOfWeek = 2,
                startTime = "11:00",
                endTime = "12:00",
                subjectName = "Database Systems"
            )
        )

        val result = useCase(slots = slots, replaceExisting = true, horizonDays = 90)

        assertTrue(result is AppResult.Success)
        assertEquals(90, fakeRepo.lastHorizonDays)
        assertEquals(true, fakeRepo.lastReplaceExisting)
    }
}
