package com.studentos.feature.intelligence.domain.usecase

import com.studentos.feature.intelligence.domain.model.DailyBriefSummaryDomain
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBriefHistoryUseCase @Inject constructor(
    private val repository: DailyBriefRepository
) {
    operator fun invoke(): Flow<List<DailyBriefSummaryDomain>> {
        return repository.getBriefSummaries()
    }
}
