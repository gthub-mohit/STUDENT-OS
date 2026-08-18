package com.studentos.feature.assignments.usecase

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.usecase.CreateAssignmentUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateAssignmentUseCaseTest {

    private class FakeAssignmentRepository : AssignmentRepository {
        var createdAssignment: AssignmentEntity? = null

        override fun getAssignmentById(id: Long): Flow<AssignmentEntity?> = error("Not needed")
        override fun getAllAssignments(): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override suspend fun getUrgentAssignments(withinEpoch: Long): AppResult<List<AssignmentEntity>> = error("Not needed")

        override suspend fun createAssignment(assignment: AssignmentEntity): AppResult<Long> {
            createdAssignment = assignment
            return AppResult.Success(100L)
        }

        override suspend fun updateStatus(id: Long, newStatus: String): AppResult<Unit> = error("Not needed")
        override suspend fun updateDeadline(id: Long, deadline: Long): AppResult<Unit> = error("Not needed")
        override suspend fun updateReminderLead(id: Long, leadMs: Long?): AppResult<Unit> = error("Not needed")
        override suspend fun deleteAssignment(id: Long): AppResult<Unit> = error("Not needed")
        override suspend fun setAttachment(id: Long, uri: String?): AppResult<Unit> = error("Not needed")
        override suspend fun attachFile(id: Long, sourceUriString: String): AppResult<String> = error("Not needed")
    }

    @Test
    fun invoke_validAssignment_returnsSuccessId() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = CreateAssignmentUseCase(repo)

        val assignment = AssignmentEntity(
            subjectId = 1L,
            title = "Math Assignment",
            deadline = System.currentTimeMillis() + 86400000L,
            createdAt = System.currentTimeMillis()
        )

        val result = useCase(assignment)
        assertTrue(result is AppResult.Success)
        assertEquals(100L, (result as AppResult.Success).data)
        assertEquals("Math Assignment", repo.createdAssignment?.title)
    }

    @Test
    fun invoke_blankTitle_returnsValidationError() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = CreateAssignmentUseCase(repo)

        val assignment = AssignmentEntity(
            subjectId = 1L,
            title = "   ",
            deadline = System.currentTimeMillis() + 86400000L,
            createdAt = System.currentTimeMillis()
        )

        val result = useCase(assignment)
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).reason is AppError.ValidationError)
    }

    @Test
    fun invoke_invalidSubjectId_returnsValidationError() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = CreateAssignmentUseCase(repo)

        val assignment = AssignmentEntity(
            subjectId = 0L,
            title = "Physics HW",
            deadline = System.currentTimeMillis() + 86400000L,
            createdAt = System.currentTimeMillis()
        )

        val result = useCase(assignment)
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).reason is AppError.ValidationError)
    }

    @Test
    fun invoke_missingDeadline_returnsValidationError() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = CreateAssignmentUseCase(repo)

        val assignment = AssignmentEntity(
            subjectId = 1L,
            title = "Physics Quiz",
            deadline = 0L,
            createdAt = System.currentTimeMillis(),
            taskType = "QUIZ"
        )

        val result = useCase(assignment)
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).reason is AppError.ValidationError)
    }

    @Test
    fun invoke_validTaskWithQuizTypeAndExactDeadline_succeeds() = runBlocking {
        val repo = FakeAssignmentRepository()
        val useCase = CreateAssignmentUseCase(repo)

        val exactDeadline = 1755500000000L
        val assignment = AssignmentEntity(
            subjectId = 2L,
            title = "Data Structures Quiz 1",
            deadline = exactDeadline,
            createdAt = System.currentTimeMillis(),
            taskType = "QUIZ"
        )

        val result = useCase(assignment)
        assertTrue(result is AppResult.Success)
        assertEquals(exactDeadline, repo.createdAssignment?.deadline)
        assertEquals("QUIZ", repo.createdAssignment?.taskType)
    }
}
