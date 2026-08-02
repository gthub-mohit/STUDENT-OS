package com.studentos.feature.attendance.data

import androidx.room.withTransaction
import com.studentos.core.database.AppDatabase
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.dao.TimetableSlotDao
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
}
