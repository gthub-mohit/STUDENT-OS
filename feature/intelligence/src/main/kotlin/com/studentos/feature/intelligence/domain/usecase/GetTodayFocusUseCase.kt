package com.studentos.feature.intelligence.domain.usecase

import com.studentos.feature.intelligence.domain.model.TodayFocusItem
import com.studentos.feature.intelligence.domain.repository.HomeOverviewRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * GetTodayFocusUseCase — Observes today's actionable priorities (up to 3 items)
 * directly from [HomeOverviewRepository].
 */
class GetTodayFocusUseCase @Inject constructor(
    private val repository: HomeOverviewRepository
) {
    operator fun invoke(): Flow<List<TodayFocusItem>> = repository.getTodayFocusItems()
}
