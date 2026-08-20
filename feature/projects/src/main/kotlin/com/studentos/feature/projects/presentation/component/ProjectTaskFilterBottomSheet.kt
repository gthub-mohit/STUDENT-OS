package com.studentos.feature.projects.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.studentos.feature.projects.presentation.state.ProjectTaskStatusFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectTaskFilterBottomSheet(
    currentFilter: ProjectTaskStatusFilter,
    onDismiss: () -> Unit,
    onApply: (ProjectTaskStatusFilter) -> Unit,
    onClearAll: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFilter by remember(currentFilter) { mutableStateOf(currentFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Filter Tasks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "STATUS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == ProjectTaskStatusFilter.ALL,
                    onClick = { selectedFilter = ProjectTaskStatusFilter.ALL },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedFilter == ProjectTaskStatusFilter.AVAILABLE,
                    onClick = { selectedFilter = ProjectTaskStatusFilter.AVAILABLE },
                    label = { Text("Available") }
                )
                FilterChip(
                    selected = selectedFilter == ProjectTaskStatusFilter.BLOCKED,
                    onClick = { selectedFilter = ProjectTaskStatusFilter.BLOCKED },
                    label = { Text("Blocked") }
                )
                FilterChip(
                    selected = selectedFilter == ProjectTaskStatusFilter.COMPLETED,
                    onClick = { selectedFilter = ProjectTaskStatusFilter.COMPLETED },
                    label = { Text("Completed") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        selectedFilter = ProjectTaskStatusFilter.ALL
                        onClearAll()
                    }
                ) {
                    Text("Clear All")
                }

                Button(
                    onClick = {
                        onApply(selectedFilter)
                    }
                ) {
                    Text("Apply")
                }
            }
        }
    }
}
