package com.studentos.feature.intelligence.domain.usecase

import com.studentos.feature.intelligence.domain.model.ComingUpItem
import com.studentos.feature.intelligence.domain.model.TodayFocusItem
import com.studentos.feature.intelligence.domain.repository.HomeOverviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * GetComingUpUseCase — Observes up to 3 upcoming deadlines, classes, CP contests,
 * or project milestones from [HomeOverviewRepository], deduplicated against [GetTodayFocusUseCase].
 *
 * Items already represented in Today's Focus are excluded from Coming Up using stable entity IDs.
 */
class GetComingUpUseCase @Inject constructor(
    private val repository: HomeOverviewRepository,
    private val getTodayFocusUseCase: GetTodayFocusUseCase
) {
    operator fun invoke(): Flow<List<ComingUpItem>> {
        return combine(
            repository.getComingUpItems(),
            getTodayFocusUseCase()
        ) { comingUpItems, todayFocusItems ->
            val todayFocusKeys = todayFocusItems.mapNotNull { it.toEntityKey() }.toSet()

            comingUpItems.filterNot { item ->
                val itemKey = item.toEntityKey()
                itemKey != null && todayFocusKeys.contains(itemKey)
            }
        }
    }

    private fun TodayFocusItem.toEntityKey(): Pair<String, Long>? {
        val normCategory = normalizeCategory(category)
        val eid = entityId ?: id.substringAfterLast('_').toLongOrNull()
        return if (eid != null) Pair(normCategory, eid) else null
    }

    private fun ComingUpItem.toEntityKey(): Pair<String, Long>? {
        val normCategory = normalizeCategory(category)
        val eid = entityId ?: id.substringAfterLast('_').toLongOrNull()
        return if (eid != null) Pair(normCategory, eid) else null
    }

    private fun normalizeCategory(category: String): String {
        return when (category.uppercase()) {
            "ATTENDANCE", "CLASS" -> "CLASS"
            "ASSIGNMENT", "TASK" -> "ASSIGNMENT"
            "DSA", "CODING" -> "DSA"
            "PROJECT", "PROJECTS" -> "PROJECT"
            else -> category.uppercase()
        }
    }
}
