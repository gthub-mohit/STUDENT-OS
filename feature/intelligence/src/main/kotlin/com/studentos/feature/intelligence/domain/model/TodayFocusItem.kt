package com.studentos.feature.intelligence.domain.model

/**
 * TodayFocusItem — Domain model representing an actionable priority item on the Home screen.
 *
 * @param id Unique identifier for the focus item (e.g., "asgn_1", "att_2")
 * @param title Human-readable action title (e.g., "Finish Mechanics assignment")
 * @param subtitle Optional supporting context (e.g., "Due today · 5:00 PM")
 * @param category Category identifier: "ASSIGNMENT", "ATTENDANCE", "DSA", "PROJECT"
 * @param isCompleted True if the priority task has been marked complete
 * @param actionRoute Destination route when tapping the row (e.g., "assignments/list")
 * @param entityId Underlying database entity ID for domain mutation
 */
data class TodayFocusItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val category: String,
    val isCompleted: Boolean,
    val actionRoute: String? = null,
    val entityId: Long? = null
)
