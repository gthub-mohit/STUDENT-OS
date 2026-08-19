package com.studentos.feature.assignments.presentation.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.assignments.domain.model.AssignmentFilter
import com.studentos.feature.assignments.domain.model.TaskType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFilterChips(
    selectedType: TaskType?,
    selectedStatus: String?,
    selectedDeadline: AssignmentFilter?,
    onRemoveType: () -> Unit,
    onRemoveStatus: () -> Unit,
    onRemoveDeadline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasType = selectedType != null
    val hasStatus = selectedStatus != null
    val hasDeadline = selectedDeadline != null && selectedDeadline != AssignmentFilter.ALL

    if (!hasType && !hasStatus && !hasDeadline) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hasType) {
            InputChip(
                selected = true,
                onClick = onRemoveType,
                label = { Text(selectedType!!.displayName) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove ${selectedType.displayName} filter"
                    )
                }
            )
        }

        if (hasStatus) {
            val statusLabel = when (selectedStatus) {
                AssignmentEntity.STATUS_PENDING -> "Pending"
                AssignmentEntity.STATUS_IN_PROGRESS -> "In Progress"
                AssignmentEntity.STATUS_COMPLETED -> "Completed"
                else -> selectedStatus ?: ""
            }
            InputChip(
                selected = true,
                onClick = onRemoveStatus,
                label = { Text(statusLabel) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove $statusLabel filter"
                    )
                }
            )
        }

        if (hasDeadline) {
            val deadlineLabel = when (selectedDeadline) {
                AssignmentFilter.TODAY -> "Today"
                AssignmentFilter.THIS_WEEK -> "This Week"
                AssignmentFilter.OVERDUE -> "Overdue"
                else -> ""
            }
            if (deadlineLabel.isNotEmpty()) {
                InputChip(
                    selected = true,
                    onClick = onRemoveDeadline,
                    label = { Text(deadlineLabel) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove $deadlineLabel filter"
                        )
                    }
                )
            }
        }
    }
}
