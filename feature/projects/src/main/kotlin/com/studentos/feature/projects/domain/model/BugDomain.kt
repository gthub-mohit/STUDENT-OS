package com.studentos.feature.projects.domain.model

data class BugDomain(
    val id: Long = 0,
    val projectId: Long,
    val description: String,
    val severity: String = SEVERITY_MEDIUM,
    val status: String = STATUS_OPEN
) {
    val isResolved: Boolean
        get() = status == STATUS_RESOLVED

    val severityOrder: Int
        get() = when (severity.uppercase()) {
            SEVERITY_HIGH -> 3
            SEVERITY_MEDIUM -> 2
            SEVERITY_LOW -> 1
            else -> 0
        }

    companion object {
        const val SEVERITY_LOW = "LOW"
        const val SEVERITY_MEDIUM = "MEDIUM"
        const val SEVERITY_HIGH = "HIGH"

        const val STATUS_OPEN = "OPEN"
        const val STATUS_RESOLVED = "RESOLVED"
    }
}
