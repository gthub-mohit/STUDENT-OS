package com.studentos.feature.projects.domain.model

data class ProjectDomain(
    val id: Long = 0,
    val title: String,
    val archivedAt: Long? = null,
    val inactivityThresholdDays: Int = 7,
    val lastActivityAt: Long,
    val nextActionId: Long? = null,
    val nextActionTitle: String? = null,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0
) {
    val isArchived: Boolean
        get() = archivedAt != null

    val progressPercentage: Float
        get() = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks.toFloat() * 100f).coerceIn(0f, 100f) else 0f

    fun isInactive(currentTimeMs: Long): Boolean {
        if (isArchived) return false
        val inactiveDays = (currentTimeMs - lastActivityAt) / (1000L * 60 * 60 * 24)
        return inactiveDays >= inactivityThresholdDays
    }
}
