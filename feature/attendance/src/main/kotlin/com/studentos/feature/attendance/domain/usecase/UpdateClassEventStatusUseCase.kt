package com.studentos.feature.attendance.domain.usecase

import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import javax.inject.Inject

/**
 * UpdateClassEventStatusUseCase — Domain use case for updating the attendance status of a class event.
 */
class UpdateClassEventStatusUseCase @Inject constructor(
    private val classEventRepository: ClassEventRepository
) {

    suspend operator fun invoke(
        eventId: Long,
        status: String
    ): AppResult<Unit> {
        return classEventRepository.updateStatus(eventId, status)
    }
}
