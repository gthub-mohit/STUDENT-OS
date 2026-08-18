package com.studentos.feature.intelligence.domain.model

/**
 * ComingUpItem — Domain model representing an upcoming event, class, deadline, or milestone on the Home screen.
 *
 * @param id Unique identifier
 * @param title Primary title (e.g., "Mechanics Assignment", "Digital Systems", "Codeforces Round 950")
 * @param subtitle Context info (e.g., "Due tomorrow", "10:00 AM · Room 204", "Saturday")
 * @param category Category identifier: "ASSIGNMENT", "CLASS", "CONTEST", "PROJECT"
 * @param actionRoute Navigation destination route (e.g., "assignments/list")
 * @param timestamp Epoch timestamp in milliseconds for sorting nearest items
 * @param entityId Underlying database entity ID for domain mutation / deduplication
 */
data class ComingUpItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val actionRoute: String? = null,
    val timestamp: Long = 0L,
    val entityId: Long? = null
)
