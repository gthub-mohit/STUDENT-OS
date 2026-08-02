package com.studentos.feature.attendance.data

import app.cash.turbine.test
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.data.repository.ClassEventRepositoryImpl
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
class ClassEventRepositoryImplTest {

    private val classEventDao: ClassEventDao = mockk(relaxed = true)
    private val appEventBus: AppEventBus = mockk(relaxed = true)

    private lateinit var repository: ClassEventRepositoryImpl

    @Before
    fun setUp() {
        repository = ClassEventRepositoryImpl(classEventDao, appEventBus)
    }

    @Test
    fun getEventsForSubject_returnsFlowFromDao() = runTest {
        val sampleEvent = ClassEventEntity(
            id = 1L,
            subjectId = 10L,
            scheduledAt = 1000L,
            endAt = 2000L,
            status = ClassEventEntity.STATUS_PRESENT,
            updatedAt = 1000L
        )
        every { classEventDao.getEventsForSubject(10L) } returns flowOf(listOf(sampleEvent))

        repository.getEventsForSubject(10L).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(1L, list[0].id)
            awaitComplete()
        }
    }

    @Test
    fun getAllAttendanceSummaries_returnsFlowFromDao() = runTest {
        val summary = SubjectAttendanceSummary(
            subjectId = 10L,
            subjectName = "Mathematics",
            presentCount = 8,
            absentCount = 2,
            cancelledCount = 0,
            holidayCount = 0,
            extraPresentCount = 0,
            totalHeldCount = 10,
            percentage = 80.0
        )
        every { classEventDao.getAllAttendanceSummaries() } returns flowOf(listOf(summary))

        repository.getAllAttendanceSummaries().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Mathematics", list[0].subjectName)
            assertEquals(80.0, list[0].percentage, 0.01)
            awaitComplete()
        }
    }

    @Test
    fun updateStatus_updatesDaoAndEmitsAttendanceEvents() = runTest {
        val sampleEvent = ClassEventEntity(
            id = 1L,
            subjectId = 10L,
            scheduledAt = 1000L,
            endAt = 2000L,
            status = ClassEventEntity.STATUS_PRESENT,
            updatedAt = 1000L
        )
        coEvery { classEventDao.getEventByIdOnce(1L) } returns sampleEvent

        val result = repository.updateStatus(1L, ClassEventEntity.STATUS_ABSENT)

        assertTrue(result is AppResult.Success)
        coVerify {
            classEventDao.updateStatus(1L, ClassEventEntity.STATUS_ABSENT, any())
            appEventBus.emit(AppEvent.AttendanceMarked(10L, ClassEventEntity.STATUS_ABSENT))
            appEventBus.emit(AppEvent.AttendanceUpdated(10L))
        }
    }

    @Test
    fun addExtraClass_insertsDaoAndEmitsAttendanceEvents() = runTest {
        coEvery { classEventDao.insert(any()) } returns 99L

        val result = repository.addExtraClass(
            subjectId = 10L,
            scheduledAt = 5000L,
            endAt = 6000L,
            linkedSlotId = null
        )

        assertTrue(result is AppResult.Success)
        assertEquals(99L, (result as AppResult.Success).data)
        coVerify {
            classEventDao.insert(
                match {
                    it.subjectId == 10L &&
                            it.scheduledAt == 5000L &&
                            it.endAt == 6000L &&
                            it.isExtra &&
                            it.status == ClassEventEntity.STATUS_EXTRA_CLASS
                }
            )
            appEventBus.emit(AppEvent.AttendanceMarked(10L, ClassEventEntity.STATUS_EXTRA_CLASS))
            appEventBus.emit(AppEvent.AttendanceUpdated(10L))
        }
    }
}
