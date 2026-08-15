package com.studentos.feature.attendance.data

import androidx.room.withTransaction
import com.studentos.core.database.AppDatabase
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.data.repository.TimetableRepositoryImpl
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableRepositoryImplTest {

    private val database: AppDatabase = mockk(relaxed = true)
    private val subjectDao: SubjectDao = mockk(relaxed = true)
    private val timetableSlotDao: TimetableSlotDao = mockk(relaxed = true)
    private val classEventDao: ClassEventDao = mockk(relaxed = true)

    private lateinit var repository: TimetableRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        val lambdaSlot = slot<suspend () -> Any?>()
        coEvery { database.withTransaction(capture(lambdaSlot)) } answers {
            runBlocking { lambdaSlot.captured.invoke() }
        }
        coEvery { classEventDao.getEventBySubjectAndSchedule(any(), any()) } returns null
        coEvery { timetableSlotDao.findMatchingSlot(any(), any(), any(), any()) } returns null
        coEvery { timetableSlotDao.getAllSlotsOnce() } returns emptyList()
        repository = TimetableRepositoryImpl(database, subjectDao, timetableSlotDao, classEventDao)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    @Test
    fun importTimetable_insertsSubjectsSlotsAndClassEvents() = runTest {
        val slotItem = ParsedTimetableSlot(
            subjectName = "Computer Science",
            dayOfWeek = 1,
            startTime = "09:00",
            endTime = "10:00",
            location = "Lab 1"
        )
        coEvery { subjectDao.getByName("Computer Science") } returns null
        coEvery { subjectDao.insert(any()) } returns 100L
        coEvery { timetableSlotDao.insert(any()) } returns 200L

        val result = repository.importTimetable(listOf(slotItem), replaceExisting = true, horizonDays = 7)

        assertTrue("Expected AppResult.Success but was $result", result is AppResult.Success)
        coVerify {
            timetableSlotDao.deleteAll()
            subjectDao.insert(match { it.name == "Computer Science" })
            timetableSlotDao.insert(match { it.subjectId == 100L && it.dayOfWeek == 1 })
            classEventDao.insert(any())
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Bug 4 Regression: Replace Timetable FK Constraint
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun replaceEmptyTimetable_works() = runTest {
        // No existing slots → deleteAll is safe, import proceeds
        coEvery { timetableSlotDao.getAllSlotsOnce() } returns emptyList()
        coEvery { subjectDao.getByName(any()) } returns null
        coEvery { subjectDao.insert(any()) } returns 1L
        coEvery { timetableSlotDao.insert(any()) } returns 10L

        val slot = ParsedTimetableSlot(
            subjectName = "Physics",
            dayOfWeek = 1,
            startTime = "09:00",
            endTime = "10:00"
        )

        val result = repository.importTimetable(listOf(slot), replaceExisting = true, horizonDays = 1)
        assertTrue("Expected Success but was $result", result is AppResult.Success)

        coVerify { timetableSlotDao.deleteAll() }
        coVerify { subjectDao.insert(match { it.name == "Physics" }) }
        coVerify { timetableSlotDao.insert(any()) }
    }

    @Test
    fun replaceExistingTimetable_withClassEvents_noFkViolation() = runTest {
        // Simulate existing timetable with both UNMARKED and PRESENT events
        val oldSlot = TimetableSlotEntity(
            id = 50L, subjectId = 1L, dayOfWeek = 1,
            startTime = "09:00", endTime = "10:00", validFrom = 1000L
        )
        coEvery { timetableSlotDao.getAllSlotsOnce() } returns listOf(oldSlot)
        coEvery { subjectDao.getByName("ME201") } returns SubjectEntity(id = 1L, name = "ME201")
        coEvery { timetableSlotDao.insert(any()) } returns 100L

        val newSlot = ParsedTimetableSlot(
            subjectName = "ME201",
            dayOfWeek = 2,
            startTime = "10:00",
            endTime = "11:00",
            location = "C003"
        )

        val result = repository.importTimetable(listOf(newSlot), replaceExisting = true, horizonDays = 1)
        assertTrue("Expected Success but was $result", result is AppResult.Success)

        // Verify correct dependency-aware deletion order:
        // 1. Delete UNMARKED events first
        coVerify { classEventDao.deleteUnmarkedBySlotIds(listOf(50L)) }
        // 2. Nullify FKs on marked events
        coVerify { classEventDao.nullifySlotReferences(listOf(50L), any()) }
        // 3. Then delete old slots
        coVerify { timetableSlotDao.deleteAll() }
        // 4. Reuse existing subject by name (id=1)
        coVerify(exactly = 0) { subjectDao.insert(any()) }
        // 5. Insert new slot with reused subject
        coVerify { timetableSlotDao.insert(match { it.subjectId == 1L && it.dayOfWeek == 2 }) }
    }

    @Test
    fun replaceExistingTimetable_withMarkedAttendance_preservesHistory() = runTest {
        // Old timetable has slots with marked attendance (PRESENT/ABSENT).
        // After replacement, marked events survive with their subject_id intact.
        val oldSlot = TimetableSlotEntity(
            id = 77L, subjectId = 5L, dayOfWeek = 3,
            startTime = "14:00", endTime = "15:00", validFrom = 2000L
        )
        coEvery { timetableSlotDao.getAllSlotsOnce() } returns listOf(oldSlot)
        coEvery { subjectDao.getByName("CS203") } returns SubjectEntity(id = 5L, name = "CS203")
        coEvery { timetableSlotDao.insert(any()) } returns 200L

        val newSlot = ParsedTimetableSlot(
            subjectName = "CS203",
            dayOfWeek = 4,
            startTime = "09:00",
            endTime = "10:00"
        )

        val result = repository.importTimetable(listOf(newSlot), replaceExisting = true, horizonDays = 1)
        assertTrue("Expected Success but was $result", result is AppResult.Success)

        // Verify marked events were detached (not deleted):
        coVerify { classEventDao.nullifySlotReferences(listOf(77L), any()) }
        // Verify subject identity was reused (not deleted or recreated):
        coVerify(exactly = 0) { subjectDao.insert(any()) }
        // New slot uses same subject
        coVerify { timetableSlotDao.insert(match { it.subjectId == 5L }) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Recurring Weekly Timetable Template & Date-Specific Event Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun importTimetable_recurringTemplate_populatesFutureWeeksWithoutTemplateDuplication() = runTest {
        // When importing a timetable with a 14-day horizon (2 weeks):
        // 1. A single template definition is inserted in timetable_slots
        // 2. Exactly 2 date-specific class events are generated (one per week)
        val slot = ParsedTimetableSlot(
            subjectName = "ME201",
            dayOfWeek = 1, // Monday
            startTime = "09:00",
            endTime = "09:55",
            location = "C003"
        )
        coEvery { subjectDao.getByName("ME201") } returns null
        coEvery { subjectDao.insert(any()) } returns 10L
        coEvery { timetableSlotDao.insert(any()) } returns 100L
        coEvery { classEventDao.getEventBySubjectAndSchedule(any(), any()) } returns null

        val result = repository.importTimetable(listOf(slot), replaceExisting = true, horizonDays = 14)
        assertTrue(result is AppResult.Success)

        // Template slot inserted only once
        coVerify(exactly = 1) {
            timetableSlotDao.insert(match { it.subjectId == 10L && it.dayOfWeek == 1 && it.startTime == "09:00" })
        }

        // Exactly 2 class events inserted (one for each Monday in the 14-day window)
        coVerify(exactly = 2) {
            classEventDao.insert(match { it.subjectId == 10L && it.timetableSlotId == 100L && it.status == "UNMARKED" })
        }
    }

    @Test
    fun importTimetable_duplicateSlotsInInput_deduplicatedIntoSingleTemplate() = runTest {
        // If the OCR / input produces duplicate slot definitions for the same subject/day/time,
        // they must be collapsed into a single recurring template slot.
        val slot1 = ParsedTimetableSlot(
            subjectName = "CS203",
            dayOfWeek = 2,
            startTime = "10:00",
            endTime = "10:55"
        )
        val slot2 = ParsedTimetableSlot(
            subjectName = "CS203",
            dayOfWeek = 2,
            startTime = "10:00",
            endTime = "10:55"
        )
        coEvery { subjectDao.getByName("CS203") } returns null
        coEvery { subjectDao.insert(any()) } returns 20L
        coEvery { timetableSlotDao.insert(any()) } returns 200L
        coEvery { classEventDao.getEventBySubjectAndSchedule(any(), any()) } returns null

        val result = repository.importTimetable(listOf(slot1, slot2), replaceExisting = true, horizonDays = 7)
        assertTrue(result is AppResult.Success)

        // Only 1 template slot inserted despite 2 duplicate entries in input
        coVerify(exactly = 1) {
            timetableSlotDao.insert(match { it.subjectId == 20L && it.dayOfWeek == 2 })
        }
    }

    @Test
    fun importTimetable_existingMarkedEvents_notOverwrittenOrDuplicated() = runTest {
        // If an event at a scheduled date is already marked (e.g. PRESENT), it must not be overwritten
        val slot = ParsedTimetableSlot(
            subjectName = "MA203",
            dayOfWeek = 3, // Wednesday
            startTime = "11:00",
            endTime = "11:55"
        )
        val existingMarkedEvent = com.studentos.core.database.entity.ClassEventEntity(
            id = 555L,
            subjectId = 30L,
            scheduledAt = 123456789L,
            endAt = 123460389L,
            status = com.studentos.core.database.entity.ClassEventEntity.STATUS_PRESENT,
            updatedAt = 1000L
        )

        coEvery { subjectDao.getByName("MA203") } returns null
        coEvery { subjectDao.insert(any()) } returns 30L
        coEvery { timetableSlotDao.insert(any()) } returns 300L
        // Return existing marked event for all lookups
        coEvery { classEventDao.getEventBySubjectAndSchedule(eq(30L), any()) } returns existingMarkedEvent

        val result = repository.importTimetable(listOf(slot), replaceExisting = true, horizonDays = 14)
        assertTrue(result is AppResult.Success)

        // No new class events should be inserted because existing marked events already exist for all occurrences
        coVerify(exactly = 0) {
            classEventDao.insert(any())
        }
    }
}
