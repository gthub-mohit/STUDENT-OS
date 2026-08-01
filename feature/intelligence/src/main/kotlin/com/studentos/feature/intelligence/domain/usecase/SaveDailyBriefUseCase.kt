package com.studentos.feature.intelligence.domain.usecase

import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import javax.inject.Inject

class SaveDailyBriefUseCase @Inject constructor(
    private val repository: DailyBriefRepository
) {
    suspend operator fun invoke(brief: DailyBrief): Long {
        return repository.saveBrief(brief)
    }
}
