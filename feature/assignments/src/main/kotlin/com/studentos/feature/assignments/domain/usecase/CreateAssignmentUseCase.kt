package com.studentos.feature.assignments.domain.usecase

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import javax.inject.Inject

/**
 * CreateAssignmentUseCase — Domain use case for validating and creating a new assignment.
 */
class CreateAssignmentUseCase @Inject constructor(
    private val assignmentRepository: AssignmentRepository
) {
    suspend operator fun invoke(assignment: AssignmentEntity): AppResult<Long> {
        if (assignment.title.isBlank()) {
            return AppResult.Failure(AppError.ValidationError("Assignment title cannot be empty"))
        }
        if (assignment.subjectId <= 0L) {
            return AppResult.Failure(AppError.ValidationError("Valid subject must be selected"))
        }
        return assignmentRepository.createAssignment(assignment)
    }
}
