package com.studentos.feature.intelligence.domain.model.fact

data class AssignmentItemFact(
    val id: Long,
    val title: String,
    val deadlineEpochMs: Long,
    val isOverdue: Boolean,
    val isUrgent: Boolean
)

data class AssignmentFact(
    val overdueCount: Int = 0,
    val urgentAssignments: List<AssignmentItemFact> = emptyList(),
    val totalPendingCount: Int = 0
)
