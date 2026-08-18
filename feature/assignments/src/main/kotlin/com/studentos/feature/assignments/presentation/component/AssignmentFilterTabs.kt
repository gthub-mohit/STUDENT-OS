package com.studentos.feature.assignments.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studentos.feature.assignments.domain.model.AssignmentFilter
import com.studentos.feature.assignments.domain.model.TaskType

@Composable
fun AssignmentFilterTabs(
    selectedFilter: AssignmentFilter,
    onFilterSelected: (AssignmentFilter) -> Unit,
    selectedType: TaskType?,
    onTypeSelected: (TaskType?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isTypeMenuExpanded by remember { mutableStateOf(false) }
    val statusScrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Compact Type Dropdown Picker
        Box {
            FilterChip(
                selected = selectedType != null,
                onClick = { isTypeMenuExpanded = true },
                label = {
                    Text(selectedType?.displayName ?: "All Types")
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select task type"
                    )
                }
            )

            DropdownMenu(
                expanded = isTypeMenuExpanded,
                onDismissRequest = { isTypeMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All Types") },
                    onClick = {
                        onTypeSelected(null)
                        isTypeMenuExpanded = false
                    }
                )
                TaskType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            onTypeSelected(type)
                            isTypeMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        VerticalDivider(modifier = Modifier.height(28.dp))
        Spacer(modifier = Modifier.width(8.dp))

        // 2. Status / Timeline Filter Chips with Scroll Overflow Gradient
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(statusScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
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

            // Trailing edge fade indicator when row overflows and can scroll forward
            if (statusScrollState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(24.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }
        }
    }
}
