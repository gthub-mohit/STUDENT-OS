package com.studentos.feature.projects.domain.model

data class ProjectTaskDomain(
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val isNextAction: Boolean = false,
    val isParallel: Boolean = false,
    val completedAt: Long? = null,
    val sortOrder: Int = 0
) {
    val isCompleted: Boolean
        get() = completedAt != null
}
