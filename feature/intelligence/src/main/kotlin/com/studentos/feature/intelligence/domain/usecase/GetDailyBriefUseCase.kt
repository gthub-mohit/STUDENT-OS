package com.studentos.feature.intelligence.domain.usecase

import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDailyBriefUseCase @Inject constructor(
    private val repository: DailyBriefRepository
) {
    operator fun invoke(date: String): Flow<DailyBrief?> {
        return repository.getBriefForDate(date)
    }
}
