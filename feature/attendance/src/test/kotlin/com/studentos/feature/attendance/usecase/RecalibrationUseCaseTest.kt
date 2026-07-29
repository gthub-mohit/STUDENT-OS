package com.studentos.feature.attendance.usecase

import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import com.studentos.feature.attendance.domain.usecase.RecalibrationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecalibrationUseCaseTest {

    private class FakeSuccessRepository : ClassEventRepository {
        override fun getEventsForSubject(subjectId: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")

        override fun getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>> {
            return flowOf(
                listOf(
                    SubjectAttendanceSummary(
                        subjectId = 1,
                        subjectName = "Physics",
                        presentCount = 10,
                        absentCount = 2,
                        cancelledCount = 0,
                        holidayCount = 0,
                        extraPresentCount = 0,
                        totalHeldCount = 12,
                        percentage = 83.33
                    )
                )
            )
        }

        override suspend fun updateStatus(eventId: Long, status: String): AppResult<Unit> = error("Not needed")
        override suspend fun addExtraClass(
            subjectId: Long,
            scheduledAt: Long,
            endAt: Long,
            linkedSlotId: Long?
        ): AppResult<Long> = error("Not needed")
    }

    private class FakeFailingRepository : ClassEventRepository {
        override fun getEventsForSubject(subjectId: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")

        override fun getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>> {
            return flow {
                throw RuntimeException("Database read disk error")
            }
        }

        override suspend fun updateStatus(eventId: Long, status: String): AppResult<Unit> = error("Not needed")
        override suspend fun addExtraClass(
            subjectId: Long,
            scheduledAt: Long,
            endAt: Long,
            linkedSlotId: Long?
        ): AppResult<Long> = error("Not needed")
    }

    @Test
    fun invoke_returnsSuccessWhenSummariesLoad() = runBlocking {
        val repo = FakeSuccessRepository()
        val useCase = RecalibrationUseCase(repo)

        val result = useCase()

        assertTrue(result is AppResult.Success)
    }

    @Test
    fun invoke_returnsFailureWhenDatabaseErrorOccurs() = runBlocking {
        val repo = FakeFailingRepository()
        val useCase = RecalibrationUseCase(repo)

        val result = useCase()

        assertTrue(result is AppResult.Failure)
        val failure = result as AppResult.Failure
        val dbErr = failure.reason as AppError.DatabaseError
        assertEquals("Database read disk error", dbErr.message)
    }
}
