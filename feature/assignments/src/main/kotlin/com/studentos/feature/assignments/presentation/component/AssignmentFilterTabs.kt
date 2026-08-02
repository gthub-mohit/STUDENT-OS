package com.studentos.feature.assignments.presentation.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studentos.feature.assignments.domain.model.AssignmentFilter

@Composable
fun AssignmentFilterTabs(
    selectedFilter: AssignmentFilter,
    onFilterSelected: (AssignmentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssignmentFilter.entries.forEach { filter ->
            val label = when (filter) {
                AssignmentFilter.ALL -> "All"
                AssignmentFilter.PENDING -> "Pending"
                AssignmentFilter.IN_PROGRESS -> "In Progress"
                AssignmentFilter.TODAY -> "Today"
                AssignmentFilter.THIS_WEEK -> "This Week"
                AssignmentFilter.OVERDUE -> "Overdue"
                AssignmentFilter.COMPLETED -> "Completed"
            }
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(label) }
            )
        }
    }
}
