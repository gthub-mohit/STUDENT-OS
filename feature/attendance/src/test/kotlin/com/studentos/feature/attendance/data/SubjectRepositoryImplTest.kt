package com.studentos.feature.attendance.data

import app.cash.turbine.test
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.data.repository.SubjectRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectRepositoryImplTest {

    private val subjectDao: SubjectDao = mockk(relaxed = true)
    private lateinit var repository: SubjectRepositoryImpl

    @Before
    fun setUp() {
        repository = SubjectRepositoryImpl(subjectDao)
    }

    @Test
    fun getActiveSubjects_returnsFlowFromDao() = runTest {
        val subject = SubjectEntity(id = 1L, name = "Physics")
        every { subjectDao.getActiveSubjects() } returns flowOf(listOf(subject))

        repository.getActiveSubjects().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Physics", list[0].name)
            awaitComplete()
        }
    }

    @Test
    fun addSubject_validatesNameAndInsertsDao() = runTest {
        coEvery { subjectDao.getByName("Chemistry") } returns null
        coEvery { subjectDao.insert(any()) } returns 5L

        val result = repository.addSubject("  Chemistry  ")

        assertTrue(result is AppResult.Success)
        assertEquals(5L, (result as AppResult.Success).data)
        coVerify {
            subjectDao.insert(match { it.name == "Chemistry" })
        }
    }

    @Test
    fun addSubject_returnsFailureWhenSubjectNameIsEmpty() = runTest {
        val result = repository.addSubject("   ")

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun renameSubject_validatesNameAndCallsDaoRename() = runTest {
        val result = repository.renameSubject(1L, "Quantum Mechanics")

        assertTrue(result is AppResult.Success)
        coVerify {
            subjectDao.rename(1L, "Quantum Mechanics")
        }
    }

    @Test
    fun archiveSubject_callsDaoArchive() = runTest {
        val result = repository.archiveSubject(1L)

        assertTrue(result is AppResult.Success)
        coVerify {
            subjectDao.archive(1L, any())
        }
    }

    @Test
    fun cleanupInvalidOcrSubjects_removesInvalidSubjects_andUnmarkedEvents() = runTest {
        val timetableSlotDao: com.studentos.core.database.dao.TimetableSlotDao = mockk(relaxed = true)
        val classEventDao: com.studentos.core.database.dao.ClassEventDao = mockk(relaxed = true)
        val repo = SubjectRepositoryImpl(
            subjectDao = subjectDao,
            timetableSlotDao = timetableSlotDao,
            classEventDao = classEventDao
        )

        val invalid1 = SubjectEntity(id = 101L, name = "C003")
        val invalid2 = SubjectEntity(id = 102L, name = "Enire cass")
        val invalid3 = SubjectEntity(id = 103L, name = "Entre")

        coEvery { subjectDao.getByNames(any()) } returns listOf(invalid1, invalid2, invalid3)
        coEvery { timetableSlotDao.getSlotIdsForSubject(101L) } returns listOf(1001L)
        coEvery { timetableSlotDao.getSlotIdsForSubject(102L) } returns listOf(1002L)
        coEvery { timetableSlotDao.getSlotIdsForSubject(103L) } returns emptyList()
        coEvery { classEventDao.countEventsForSubject(any()) } returns 0

        val result = repo.cleanupInvalidOcrSubjects(listOf("C003", "Enire cass", "Entre"))

        assertTrue(result is AppResult.Success)
        coVerify {
            classEventDao.deleteUnmarkedBySlotIds(listOf(1001L))
            classEventDao.deleteUnmarkedBySlotIds(listOf(1002L))
            timetableSlotDao.deleteBySubjectId(101L)
            timetableSlotDao.deleteBySubjectId(102L)
            subjectDao.deleteById(101L)
            subjectDao.deleteById(102L)
            subjectDao.deleteById(103L)
        }
    }

    @Test
    fun cleanupInvalidOcrSubjects_preservesValidSubjects() = runTest {
        val timetableSlotDao: com.studentos.core.database.dao.TimetableSlotDao = mockk(relaxed = true)
        val classEventDao: com.studentos.core.database.dao.ClassEventDao = mockk(relaxed = true)
        val repo = SubjectRepositoryImpl(
            subjectDao = subjectDao,
            timetableSlotDao = timetableSlotDao,
            classEventDao = classEventDao
        )

        // DB only contains valid subjects
        coEvery { subjectDao.getByNames(any()) } returns emptyList()

        val result = repo.cleanupInvalidOcrSubjects(listOf("C003", "Enire cass", "Entre"))

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 0) {
            subjectDao.deleteById(any())
            timetableSlotDao.deleteBySubjectId(any())
            classEventDao.deleteUnmarkedBySubjectId(any())
        }
    }

    @Test
    fun cleanupInvalidOcrSubjects_withExistingMarkedAttendance_preservesAttendanceHistory() = runTest {
        val timetableSlotDao: com.studentos.core.database.dao.TimetableSlotDao = mockk(relaxed = true)
        val classEventDao: com.studentos.core.database.dao.ClassEventDao = mockk(relaxed = true)
        val repo = SubjectRepositoryImpl(
            subjectDao = subjectDao,
            timetableSlotDao = timetableSlotDao,
            classEventDao = classEventDao
        )

        val invalidSubject = SubjectEntity(id = 201L, name = "C003")
        coEvery { subjectDao.getByNames(any()) } returns listOf(invalidSubject)
        coEvery { timetableSlotDao.getSlotIdsForSubject(201L) } returns listOf(2001L)
        // User had marked 1 class as PRESENT
        coEvery { classEventDao.countMarkedEventsForSubject(201L) } returns 1

        val result = repo.cleanupInvalidOcrSubjects(listOf("C003", "Enire cass", "Entre"))

        assertTrue(result is AppResult.Success)
        // Since marked attendance exists, subject row is preserved (archived) rather than deleting and violating FK
        coVerify(exactly = 0) {
            subjectDao.deleteById(201L)
        }
        coVerify {
            classEventDao.deleteUnmarkedBySlotIds(listOf(2001L))
            classEventDao.nullifySlotReferences(listOf(2001L), any())
            timetableSlotDao.deleteBySubjectId(201L)
            subjectDao.archive(201L, any())
        }
    }

    @Test
    fun cleanupInvalidOcrSubjects_idempotent_runningTwiceDoesNotFail() = runTest {
        val timetableSlotDao: com.studentos.core.database.dao.TimetableSlotDao = mockk(relaxed = true)
        val classEventDao: com.studentos.core.database.dao.ClassEventDao = mockk(relaxed = true)
        val repo = SubjectRepositoryImpl(
            subjectDao = subjectDao,
            timetableSlotDao = timetableSlotDao,
            classEventDao = classEventDao
        )

        val invalid1 = SubjectEntity(id = 101L, name = "C003")
        coEvery { subjectDao.getByNames(any()) } returnsMany listOf(
            listOf(invalid1),
            emptyList()
        )
        coEvery { timetableSlotDao.getSlotIdsForSubject(101L) } returns emptyList()
        coEvery { classEventDao.countEventsForSubject(101L) } returns 0

        val firstRun = repo.cleanupInvalidOcrSubjects()
        val secondRun = repo.cleanupInvalidOcrSubjects()

        assertTrue(firstRun is AppResult.Success)
        assertTrue(secondRun is AppResult.Success)
    }

    @Test
    fun cleanupInvalidOcrSubjects_existingC003Subject_isActuallyDeletedFromDatabase() = runTest {
        val timetableSlotDao: com.studentos.core.database.dao.TimetableSlotDao = mockk(relaxed = true)
        val classEventDao: com.studentos.core.database.dao.ClassEventDao = mockk(relaxed = true)
        val repo = SubjectRepositoryImpl(
            subjectDao = subjectDao,
            timetableSlotDao = timetableSlotDao,
            classEventDao = classEventDao
        )

        val c003Subject = SubjectEntity(id = 301L, name = "C003")
        val c003SlotId = 4001L

        // C003 exists with a timetable slot and unmarked events (standard OCR mistake)
        coEvery { subjectDao.getByNames(any()) } returns listOf(c003Subject)
        coEvery { timetableSlotDao.getSlotIdsForSubject(301L) } returns listOf(c003SlotId)
        coEvery { classEventDao.countMarkedEventsForSubject(301L) } returns 0

        val result = repo.cleanupInvalidOcrSubjects(listOf("C003"))

        assertTrue(result is AppResult.Success)

        // 1. Unmarked events for the slot are deleted
        coVerify(exactly = 1) { classEventDao.deleteUnmarkedBySlotIds(listOf(c003SlotId)) }
        // 2. Timetable slot is deleted
        coVerify(exactly = 1) { timetableSlotDao.deleteBySubjectId(301L) }
        // 3. Any remaining unmarked events for the subject are deleted
        coVerify { classEventDao.deleteUnmarkedBySubjectId(301L) }
        // 4. The C003 SubjectEntity is DELETED directly from the database table (not archived/hidden)
        coVerify(exactly = 1) { subjectDao.deleteById(301L) }
        coVerify(exactly = 0) { subjectDao.archive(301L, any()) }
    }
}
