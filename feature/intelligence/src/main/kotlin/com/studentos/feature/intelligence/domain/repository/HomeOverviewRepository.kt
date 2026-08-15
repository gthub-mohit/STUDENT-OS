package com.studentos.feature.intelligence.domain.repository

import com.studentos.feature.intelligence.domain.model.ComingUpItem
import com.studentos.feature.intelligence.domain.model.TodayFocusItem
import kotlinx.coroutines.flow.Flow

/**
 * HomeOverviewRepository — Domain repository contract for aggregating Home screen overview data.
 *
 * Responsibilities:
 * - Emits today's actionable priorities (max 3) from real feature data.
 * - Emits nearest upcoming events/deadlines/classes (max 3).
 *
 * Mutation operations for individual items are handled by dedicated feature repository contracts.
 */
interface HomeOverviewRepository {

    /**
     * Observes up to 3 actionable priorities for today.
     * Derived from real assignments, scheduled classes, DSA revision topics, and project next actions.
     */
    fun getTodayFocusItems(): Flow<List<TodayFocusItem>>

    /**
     * Observes up to 3 nearest upcoming deadlines, classes, CP contests, or project milestones.
     */
    fun getComingUpItems(): Flow<List<ComingUpItem>>
}
