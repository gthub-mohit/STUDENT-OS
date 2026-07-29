package com.studentos.feature.attendance.domain.usecase

import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import javax.inject.Inject

/**
 * AddExtraClassUseCase — Domain use case for adding an extra class event.
 */
class AddExtraClassUseCase @Inject constructor(
    private val classEventRepository: ClassEventRepository
) {

    suspend operator fun invoke(
        subjectId: Long,
        scheduledAt: Long,
        endAt: Long,
        linkedSlotId: Long? = null
    ): AppResult<Long> {
        return classEventRepository.addExtraClass(
            subjectId = subjectId,
            scheduledAt = scheduledAt,
            endAt = endAt,
            linkedSlotId = linkedSlotId
        )
    }
}
