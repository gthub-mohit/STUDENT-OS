package com.studentos.feature.assignments.usecase

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.model.AssignmentFilter
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.usecase.GetFilteredAssignmentsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFilteredAssignmentsUseCaseTest {

    private class FakeAssignmentRepository : AssignmentRepository {
        var lastTodayStart: Long? = null
        var lastTodayEnd: Long? = null
        var lastWeekStart: Long? = null
        var lastWeekEnd: Long? = null
        var lastOverdueNow: Long? = null
        var lastRequestedStatus: String? = null

        override fun getAssignmentById(id: Long): Flow<AssignmentEntity?> = error("Not needed")
        override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> {
            lastRequestedStatus = status
            return flowOf(listOf(createAssignment(104L, "Completed HW", status)))
        }

        override fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> {
            lastTodayStart = startEpoch
            lastTodayEnd = endEpoch
            return flowOf(listOf(createAssignment(101L, "Today HW", AssignmentEntity.STATUS_PENDING)))
        }

        override fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> {
            lastWeekStart = startEpoch
            lastWeekEnd = endEpoch
            return flowOf(listOf(createAssignment(102L, "Week HW", AssignmentEntity.STATUS_IN_PROGRESS)))
        }

        override fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>> {
            lastOverdueNow = nowEpoch
            return flowOf(listOf(createAssignment(103L, "Overdue HW", AssignmentEntity.STATUS_PENDING)))
        }

        override suspend fun getUrgentAssignments(withinEpoch: Long): AppResult<List<AssignmentEntity>> = error("Not needed")
        override suspend fun createAssignment(assignment: AssignmentEntity): AppResult<Long> = error("Not needed")
        override suspend fun updateStatus(id: Long, newStatus: String): AppResult<Unit> = error("Not needed")
        override suspend fun updateDeadline(id: Long, deadline: Long): AppResult<Unit> = error("Not needed")
        override suspend fun updateReminderLead(id: Long, leadMs: Long?): AppResult<Unit> = error("Not needed")
        override suspend fun deleteAssignment(id: Long): AppResult<Unit> = error("Not needed")
        override suspend fun setAttachment(id: Long, uri: String?): AppResult<Unit> = error("Not needed")
        override suspend fun attachFile(id: Long, sourceUriString: String): AppResult<String> = error("Not needed")

        private fun createAssignment(id: Long, title: String, status: String): AssignmentEntity {
            return AssignmentEntity(
                id = id,
                subjectId = 1L,
                title = title,
                deadline = System.currentTimeMillis() + 86400000L,
                status = status,
                createdAt = System.currentTimeMillis()
            )
        }
    }

    @Test
    fun invoke_todayFilter_delegatesToRepositoryTodayWithValidBoundaries() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = GetFilteredAssignmentsUseCase(repo)

        val resultList = useCase(AssignmentFilter.TODAY).first()

        assertEquals(1, resultList.size)
        assertEquals("Today HW", resultList[0].title)
        assertEquals(true, repo.lastTodayStart != null)
        assertEquals(true, repo.lastTodayEnd != null)
        assertEquals(true, (repo.lastTodayEnd ?: 0L) > (repo.lastTodayStart ?: 0L))
    }

    @Test
    fun invoke_thisWeekFilter_delegatesToRepositoryThisWeekWith7DaySpan() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = GetFilteredAssignmentsUseCase(repo)

        val resultList = useCase(AssignmentFilter.THIS_WEEK).first()

        assertEquals(1, resultList.size)
        assertEquals("Week HW", resultList[0].title)
        val diff = (repo.lastWeekEnd ?: 0L) - (repo.lastWeekStart ?: 0L)
        assertEquals(true, diff >= 7 * 86400000L - 1000L)
    }

    @Test
    fun invoke_overdueFilter_delegatesToRepositoryOverdueWithCurrentEpoch() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = GetFilteredAssignmentsUseCase(repo)

        val resultList = useCase(AssignmentFilter.OVERDUE).first()

        assertEquals(1, resultList.size)
        assertEquals("Overdue HW", resultList[0].title)
        assertEquals(true, (repo.lastOverdueNow ?: 0L) > 0L)
    }

    @Test
    fun invoke_completedFilter_delegatesToRepositoryStatusQuery() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = GetFilteredAssignmentsUseCase(repo)

        val resultList = useCase(AssignmentFilter.COMPLETED).first()

        assertEquals(1, resultList.size)
        assertEquals("Completed HW", resultList[0].title)
        assertEquals(AssignmentEntity.STATUS_COMPLETED, repo.lastRequestedStatus)
    }
}
