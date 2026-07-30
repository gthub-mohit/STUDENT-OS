package com.studentos.feature.assignments.usecase

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.usecase.UpdateAssignmentStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateAssignmentStatusUseCaseTest {

    private class FakeAssignmentRepository(
        var assignment: AssignmentEntity? = null
    ) : AssignmentRepository {
        var lastUpdatedStatus: String? = null

        override fun getAssignmentById(id: Long): Flow<AssignmentEntity?> = flowOf(assignment)
        override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override suspend fun getUrgentAssignments(withinEpoch: Long): AppResult<List<AssignmentEntity>> = error("Not needed")
        override suspend fun createAssignment(assignment: AssignmentEntity): AppResult<Long> = error("Not needed")

        override suspend fun updateStatus(id: Long, newStatus: String): AppResult<Unit> {
            lastUpdatedStatus = newStatus
            return AppResult.Success(Unit)
        }

        override suspend fun updateDeadline(id: Long, deadline: Long): AppResult<Unit> = error("Not needed")
        override suspend fun updateReminderLead(id: Long, leadMs: Long?): AppResult<Unit> = error("Not needed")
        override suspend fun deleteAssignment(id: Long): AppResult<Unit> = error("Not needed")
        override suspend fun setAttachment(id: Long, uri: String?): AppResult<Unit> = error("Not needed")
        override suspend fun attachFile(id: Long, sourceUriString: String): AppResult<String> = error("Not needed")
    }

    @Test
    fun invoke_pendingToInProgress_returnsSuccess() = runBlocking {
        val existing = AssignmentEntity(
            id = 1L,
            subjectId = 1L,
            title = "HW 1",
            deadline = System.currentTimeMillis() + 86400000L,
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = System.currentTimeMillis()
        )
        val repo = FakeAssignmentRepository(existing)
        val useCase = UpdateAssignmentStatusUseCase(repo)

        val result = useCase(1L, AssignmentEntity.STATUS_IN_PROGRESS)
        assertTrue(result is AppResult.Success)
        assertEquals(AssignmentEntity.STATUS_IN_PROGRESS, repo.lastUpdatedStatus)
    }

    @Test
    fun invoke_completedToPending_returnsValidationError() = runBlocking {
        val existing = AssignmentEntity(
            id = 1L,
            subjectId = 1L,
            title = "HW 1",
            deadline = System.currentTimeMillis() + 86400000L,
            status = AssignmentEntity.STATUS_COMPLETED,
            createdAt = System.currentTimeMillis()
        )
        val repo = FakeAssignmentRepository(existing)
        val useCase = UpdateAssignmentStatusUseCase(repo)

        val result = useCase(1L, AssignmentEntity.STATUS_PENDING)
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).reason is AppError.ValidationError)
    }
}
