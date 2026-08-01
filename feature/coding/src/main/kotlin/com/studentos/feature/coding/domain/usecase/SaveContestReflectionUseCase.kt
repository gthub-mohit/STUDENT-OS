package com.studentos.feature.coding.domain.usecase

import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.coding.domain.model.CpReflection
import com.studentos.feature.coding.domain.repository.CpRepository
import javax.inject.Inject

class SaveContestReflectionUseCase @Inject constructor(
    private val cpRepository: CpRepository,
    private val eventBus: AppEventBus
) {
    suspend operator fun invoke(reflection: CpReflection) {
        val validRating = reflection.selfRating.coerceIn(1, 5)
        val validReflection = reflection.copy(selfRating = validRating)

        cpRepository.saveReflection(validReflection)
        eventBus.emit(AppEvent.ContestReflectionAdded(reflection.contestId))
    }
}
