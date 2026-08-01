package com.studentos.feature.coding.domain.usecase

import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.coding.domain.model.DsaTopic
import com.studentos.feature.coding.domain.repository.DsaRepository
import javax.inject.Inject

class UpdateDsaTopicUseCase @Inject constructor(
    private val dsaRepository: DsaRepository,
    private val eventBus: AppEventBus
) {
    suspend operator fun invoke(topic: DsaTopic) {
        val validConfidence = topic.confidenceLevel.coerceIn(1, 5)
        val validTopic = topic.copy(confidenceLevel = validConfidence)

        dsaRepository.updateTopic(validTopic)
        eventBus.emit(AppEvent.DsaTopicUpdated(topic.id))
    }
}
