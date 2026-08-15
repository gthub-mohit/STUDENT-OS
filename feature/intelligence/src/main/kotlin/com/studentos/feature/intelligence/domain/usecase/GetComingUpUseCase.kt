package com.studentos.feature.intelligence.domain.usecase

import com.studentos.feature.intelligence.domain.model.ComingUpItem
import com.studentos.feature.intelligence.domain.repository.HomeOverviewRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * GetComingUpUseCase — Observes up to 3 upcoming deadlines, classes, CP contests,
 * or project milestones from [HomeOverviewRepository].
 */
class GetComingUpUseCase @Inject constructor(
    private val repository: HomeOverviewRepository
) {
    operator fun invoke(): Flow<List<ComingUpItem>> = repository.getComingUpItems()
}
