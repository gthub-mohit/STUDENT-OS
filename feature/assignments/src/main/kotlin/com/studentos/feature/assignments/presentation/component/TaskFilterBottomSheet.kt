package com.studentos.feature.assignments.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.assignments.domain.model.AssignmentFilter
import com.studentos.feature.assignments.domain.model.TaskType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFilterBottomSheet(
    initialType: TaskType?,
    initialStatus: String?,
    initialDeadline: AssignmentFilter?,
    onDismiss: () -> Unit,
    onApply: (type: TaskType?, status: String?, deadline: AssignmentFilter?) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var tempType by remember { mutableStateOf(initialType) }
    var tempStatus by remember { mutableStateOf(initialStatus) }
    var tempDeadline by remember { mutableStateOf(initialDeadline) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FILTERS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // 1. TYPE SECTION
            Text(
                text = "TYPE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempType == null,
                    onClick = { tempType = null },
                    label = { Text("All Types") }
                )
                FilterChip(
                    selected = tempType == TaskType.ASSIGNMENT,
                    onClick = { tempType = TaskType.ASSIGNMENT },
                    label = { Text("Assignment") }
                )
                FilterChip(
                    selected = tempType == TaskType.QUIZ,
                    onClick = { tempType = TaskType.QUIZ },
                    label = { Text("Quiz") }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempType == TaskType.LAB_RECORD,
                    onClick = { tempType = TaskType.LAB_RECORD },
                    label = { Text("Lab Record") }
                )
                FilterChip(
                    selected = tempType == TaskType.OTHER,
                    onClick = { tempType = TaskType.OTHER },
                    label = { Text("Other") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. STATUS SECTION
            Text(
                text = "STATUS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempStatus == null,
                    onClick = { tempStatus = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = tempStatus == AssignmentEntity.STATUS_PENDING,
                    onClick = { tempStatus = AssignmentEntity.STATUS_PENDING },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = tempStatus == AssignmentEntity.STATUS_IN_PROGRESS,
                    onClick = { tempStatus = AssignmentEntity.STATUS_IN_PROGRESS },
                    label = { Text("In Progress") }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempStatus == AssignmentEntity.STATUS_COMPLETED,
                    onClick = { tempStatus = AssignmentEntity.STATUS_COMPLETED },
                    label = { Text("Completed") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. DEADLINE SECTION
            Text(
                text = "DEADLINE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempDeadline == null || tempDeadline == AssignmentFilter.ALL,
                    onClick = { tempDeadline = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = tempDeadline == AssignmentFilter.TODAY,
                    onClick = { tempDeadline = AssignmentFilter.TODAY },
                    label = { Text("Today") }
                )
                FilterChip(
                    selected = tempDeadline == AssignmentFilter.THIS_WEEK,
                    onClick = { tempDeadline = AssignmentFilter.THIS_WEEK },
                    label = { Text("This Week") }
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempDeadline == AssignmentFilter.OVERDUE,
                    onClick = { tempDeadline = AssignmentFilter.OVERDUE },
                    label = { Text("Overdue") }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        tempType = null
                        tempStatus = null
                        tempDeadline = null
                    }
                ) {
                    Text("Clear All")
                }

                Button(
                    onClick = {
                        onApply(tempType, tempStatus, tempDeadline)
                        onDismiss()
                    }
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
