package com.studentos.feature.assignments.domain.usecase

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * UpdateAssignmentStatusUseCase — Domain use case for validating status transitions and updating assignment status.
 */
class UpdateAssignmentStatusUseCase @Inject constructor(
    private val assignmentRepository: AssignmentRepository
) {
    suspend operator fun invoke(id: Long, newStatus: String): AppResult<Unit> {
        val currentAssignment = assignmentRepository.getAssignmentById(id).firstOrNull()
            ?: return AppResult.Failure(AppError.ValidationError("Assignment not found"))

        val currentStatus = currentAssignment.status
        if (currentStatus == newStatus) {
            return AppResult.Success(Unit)
        }

        if (!isValidTransition(currentStatus, newStatus)) {
            return AppResult.Failure(
                AppError.ValidationError("Invalid status transition from $currentStatus to $newStatus")
            )
        }

        return assignmentRepository.updateStatus(id, newStatus)
    }

    private fun isValidTransition(current: String, next: String): Boolean {
        return when (current) {
            AssignmentEntity.STATUS_PENDING -> next in setOf(
                AssignmentEntity.STATUS_IN_PROGRESS,
                AssignmentEntity.STATUS_SUBMITTED,
                AssignmentEntity.STATUS_COMPLETED
            )
            AssignmentEntity.STATUS_IN_PROGRESS -> next in setOf(
                AssignmentEntity.STATUS_PENDING,
                AssignmentEntity.STATUS_SUBMITTED,
                AssignmentEntity.STATUS_COMPLETED
            )
            AssignmentEntity.STATUS_SUBMITTED -> next in setOf(
                AssignmentEntity.STATUS_IN_PROGRESS,
                AssignmentEntity.STATUS_COMPLETED
            )
            AssignmentEntity.STATUS_COMPLETED -> next == AssignmentEntity.STATUS_IN_PROGRESS
            else -> false
        }
    }
}
