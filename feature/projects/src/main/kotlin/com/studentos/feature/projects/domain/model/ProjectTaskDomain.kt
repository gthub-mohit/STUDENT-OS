package com.studentos.feature.projects.domain.model

enum class ProjectTaskPriority {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun fromString(value: String?): ProjectTaskPriority {
            return when (value?.uppercase()) {
                "LOW" -> LOW
                "HIGH" -> HIGH
                else -> MEDIUM
            }
        }
    }
}

enum class ProjectTaskState {
    AVAILABLE,
    BLOCKED,
    COMPLETED
}

data class ProjectTaskDomain(
    val id: Long = 0,
    val projectId: Long,
    val title: String,
    val isNextAction: Boolean = false,
    val isParallel: Boolean = false,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val dependencyTaskId: Long? = null,
    val priority: ProjectTaskPriority = ProjectTaskPriority.MEDIUM,
    val deadline: Long? = null
) {
    val isCompleted: Boolean
        get() = completedAt != null
}
