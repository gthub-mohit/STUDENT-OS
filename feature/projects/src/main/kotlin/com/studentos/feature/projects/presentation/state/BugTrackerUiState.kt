package com.studentos.feature.projects.presentation.state

import com.studentos.feature.projects.domain.model.BugDomain
import com.studentos.feature.projects.domain.model.ProjectDomain

enum class BugStatusFilter { ALL, OPEN, RESOLVED }
enum class BugSeverityFilter { ALL, HIGH, MEDIUM, LOW }
enum class BugSortOrder { SEVERITY_DESC, SEVERITY_ASC, NEWEST }

data class BugTrackerUiState(
    val isLoading: Boolean = true,
    val project: ProjectDomain? = null,
    val bugs: List<BugDomain> = emptyList(),
    val statusFilter: BugStatusFilter = BugStatusFilter.OPEN,
    val severityFilter: BugSeverityFilter = BugSeverityFilter.ALL,
    val sortOrder: BugSortOrder = BugSortOrder.SEVERITY_DESC,
    val isCreateDialogOpen: Boolean = false,
    val bugToEdit: BugDomain? = null,
    val errorMessage: String? = null
) {
    val openCount: Int
        get() = bugs.count { !it.isResolved }

    val resolvedCount: Int
        get() = bugs.count { it.isResolved }

    val filteredBugs: List<BugDomain>
        get() {
            return bugs
                .filter { bug ->
                    when (statusFilter) {
                        BugStatusFilter.ALL -> true
                        BugStatusFilter.OPEN -> !bug.isResolved
                        BugStatusFilter.RESOLVED -> bug.isResolved
                    }
                }
                .filter { bug ->
                    when (severityFilter) {
                        BugSeverityFilter.ALL -> true
                        BugSeverityFilter.HIGH -> bug.severity.equals("HIGH", ignoreCase = true)
                        BugSeverityFilter.MEDIUM -> bug.severity.equals("MEDIUM", ignoreCase = true)
                        BugSeverityFilter.LOW -> bug.severity.equals("LOW", ignoreCase = true)
                    }
                }
                .sortedWith { b1, b2 ->
                    when (sortOrder) {
                        BugSortOrder.SEVERITY_DESC -> b2.severityOrder.compareTo(b1.severityOrder)
                        BugSortOrder.SEVERITY_ASC -> b1.severityOrder.compareTo(b2.severityOrder)
                        BugSortOrder.NEWEST -> b2.id.compareTo(b1.id)
                    }
                }
        }

    val isEmpty: Boolean
        get() = !isLoading && filteredBugs.isEmpty()
}
