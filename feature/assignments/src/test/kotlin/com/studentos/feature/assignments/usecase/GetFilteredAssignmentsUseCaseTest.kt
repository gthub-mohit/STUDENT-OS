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
        override fun getAllAssignments(): Flow<List<AssignmentEntity>> = flowOf(listOf(createAssignment(100L, "All HW", AssignmentEntity.STATUS_PENDING)))
        override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> {
            lastRequestedStatus = status
            return flowOf(listOf(createAssignment(104L, "Status HW", status)))
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
                deadline = System.currentTimeMillis(),
                status = status,
                createdAt = System.currentTimeMillis()
            )
        }
    }

    @Test
    fun filterAll_queriesAllAssignments() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = GetFilteredAssignmentsUseCase(repo)

        val result = useCase(AssignmentFilter.ALL).first()
        assertEquals(1, result.size)
        assertEquals("All HW", result[0].title)
    }

    @Test
    fun filterPending_queriesPendingAssignments() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = GetFilteredAssignmentsUseCase(repo)

        val result = useCase(AssignmentFilter.PENDING).first()
        assertEquals(1, result.size)
        assertEquals(AssignmentEntity.STATUS_PENDING, repo.lastRequestedStatus)
    }

    @Test
    fun filterInProgress_queriesInProgressAssignments() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = GetFilteredAssignmentsUseCase(repo)

        val result = useCase(AssignmentFilter.IN_PROGRESS).first()
        assertEquals(1, result.size)
        assertEquals(AssignmentEntity.STATUS_IN_PROGRESS, repo.lastRequestedStatus)
    }

    @Test
    fun filterWithTaskType_filtersByBothStatusAndTaskType() = runBlocking {
        val mixedList = listOf(
            AssignmentEntity(1L, 1L, "Math Assignment", deadline = 1000L, status = AssignmentEntity.STATUS_PENDING, createdAt = 1000L, taskType = "ASSIGNMENT"),
            AssignmentEntity(2L, 1L, "Math Quiz", deadline = 2000L, status = AssignmentEntity.STATUS_PENDING, createdAt = 1000L, taskType = "QUIZ"),
            AssignmentEntity(3L, 1L, "Math Lab", deadline = 3000L, status = AssignmentEntity.STATUS_PENDING, createdAt = 1000L, taskType = "LAB_RECORD")
        )

        val repo = object : AssignmentRepository by FakeAssignmentRepository() {
            override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> = flowOf(mixedList)
        }
        val useCase = GetFilteredAssignmentsUseCase(repo)

        // Filter PENDING + QUIZ
        val quizResult = useCase(filter = AssignmentFilter.PENDING, taskType = com.studentos.feature.assignments.domain.model.TaskType.QUIZ).first()
        assertEquals(1, quizResult.size)
        assertEquals("Math Quiz", quizResult[0].title)
        assertEquals("QUIZ", quizResult[0].taskType)

        // Filter PENDING + ALL TYPES (null)
        val allTypesResult = useCase(filter = AssignmentFilter.PENDING, taskType = null).first()
        assertEquals(3, allTypesResult.size)
    }
}
