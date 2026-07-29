package com.studentos.feature.attendance.usecase

import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.usecase.UpdateClassEventStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateClassEventStatusUseCaseTest {

    private class FakeAppEventBus : AppEventBus {
        private val _events = MutableSharedFlow<AppEvent>()
        override val events: SharedFlow<AppEvent> = _events.asSharedFlow()
        val emittedEvents = mutableListOf<AppEvent>()

        override suspend fun emit(event: AppEvent) {
            emittedEvents.add(event)
            _events.emit(event)
        }
    }

    private class FakeClassEventRepository(
        private val bus: AppEventBus
    ) : com.studentos.feature.attendance.domain.repository.ClassEventRepository {
        var lastUpdatedId: Long? = null
        var lastUpdatedStatus: String? = null

        override fun getEventsForSubject(subjectId: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>> = error("Not needed")

        override suspend fun updateStatus(eventId: Long, status: String): AppResult<Unit> {
            lastUpdatedId = eventId
            lastUpdatedStatus = status
            bus.emit(AppEvent.AttendanceMarked(subjectId = 101L, status = status))
            return AppResult.Success(Unit)
        }

        override suspend fun addExtraClass(
            subjectId: Long,
            scheduledAt: Long,
            endAt: Long,
            linkedSlotId: Long?
        ): AppResult<Long> = error("Not needed")
    }

    @Test
    fun invoke_updatesStatusAndEmitsEvent() = runBlocking {
        val bus = FakeAppEventBus()
        val repo = FakeClassEventRepository(bus)
        val useCase = UpdateClassEventStatusUseCase(repo)

        val result = useCase(eventId = 42L, status = ClassEventEntity.STATUS_PRESENT)

        assertTrue(result is AppResult.Success)
        assertEquals(42L, repo.lastUpdatedId)
        assertEquals(ClassEventEntity.STATUS_PRESENT, repo.lastUpdatedStatus)
        assertEquals(1, bus.emittedEvents.size)
        assertTrue(bus.emittedEvents[0] is AppEvent.AttendanceMarked)
        val marked = bus.emittedEvents[0] as AppEvent.AttendanceMarked
        assertEquals(101L, marked.subjectId)
        assertEquals(ClassEventEntity.STATUS_PRESENT, marked.status)
    }
}
