package com.studentos.feature.intelligence.domain.usecase

import com.studentos.feature.intelligence.domain.generator.DailyBriefGenerator
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import javax.inject.Inject

class GenerateDailyBriefUseCase @Inject constructor(
    private val generator: DailyBriefGenerator,
    private val repository: DailyBriefRepository
) {
    suspend operator fun invoke(todayDate: String): DailyBrief {
        val brief = generator.generateBrief(todayDate)
        repository.saveBrief(brief)
        return brief
    }
}
