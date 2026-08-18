package com.studentos.feature.assignments.domain.model

import com.studentos.core.database.entity.AssignmentEntity

enum class UrgencyCategory(val displayName: String) {
    OVERDUE("Overdue"),
    DUE_TODAY("Due Today"),
    DUE_TOMORROW("Due Tomorrow"),
    DUE_THIS_WEEK("Due This Week"),
    LATER("Later")
}

data class PrioritizedAssignmentGroup(
    val category: UrgencyCategory,
    val assignments: List<AssignmentEntity>
)
