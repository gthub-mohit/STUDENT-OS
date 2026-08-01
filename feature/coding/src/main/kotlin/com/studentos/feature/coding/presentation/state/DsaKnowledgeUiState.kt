package com.studentos.feature.coding.presentation.state

import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic

data class CategoryWithTopics(
    val category: DsaCategory,
    val topics: List<DsaTopic>,
    val isExpanded: Boolean = true
) {
    val completionPercentage: Float
        get() {
            if (topics.isEmpty()) return 0f
            val revisedCount = topics.count { it.revisionStatus == DsaTopic.STATUS_REVISED }
            return (revisedCount.toFloat() / topics.size) * 100f
        }
}

data class DsaKnowledgeUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryWithTopics> = emptyList(),
    val expandedCategoryIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
    val showAddCategoryDialog: Boolean = false,
    val categoryToDelete: DsaCategory? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && categories.isEmpty()

    val overallTreeProgress: Float
        get() {
            val allTopics = categories.flatMap { it.topics }
            if (allTopics.isEmpty()) return 0f
            val revised = allTopics.count { it.revisionStatus == DsaTopic.STATUS_REVISED }
            return (revised.toFloat() / allTopics.size) * 100f
        }
}
