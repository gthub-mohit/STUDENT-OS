package com.studentos.feature.assignments.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.assignments.presentation.component.AssignmentCard
import com.studentos.feature.assignments.presentation.component.AssignmentFilterTabs
import com.studentos.feature.assignments.presentation.component.PrioritizedAssignmentList
import com.studentos.feature.assignments.presentation.state.AssignmentListUiState
import com.studentos.feature.assignments.presentation.viewmodel.AssignmentListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentListScreen(
    viewModel: AssignmentListViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var isPrioritizedView by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assignments & Deadlines") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Assignment")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // View Mode Toggle (Filtered List vs Urgent Prioritized View)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isPrioritizedView,
                    onClick = { isPrioritizedView = false },
                    label = { Text("Filtered View") }
                )
                FilterChip(
                    selected = isPrioritizedView,
                    onClick = { isPrioritizedView = true },
                    label = { Text("Urgent & Prioritized") }
                )
            }

            if (!isPrioritizedView) {
                AssignmentFilterTabs(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { viewModel.selectFilter(it) }
                )
            }

            when (val state = uiState) {
                is AssignmentListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AssignmentListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is AssignmentListUiState.Success -> {
                    if (isPrioritizedView) {
                        if (state.prioritizedGroups.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No upcoming deadlines.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            PrioritizedAssignmentList(
                                groups = state.prioritizedGroups,
                                subjectsMap = state.subjectsMap,
                                onAssignmentClick = onNavigateToDetail,
                                onDeleteClick = { viewModel.requestDelete(it) },
                                onStatusClick = { viewModel.cycleStatus(it) },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        if (state.assignments.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No assignments found for this filter.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(state.assignments, key = { it.id }) { assignment ->
                                    val subjectName = state.subjectsMap[assignment.subjectId] ?: "Subject ${assignment.subjectId}"
                                    AssignmentCard(
                                        assignment = assignment,
                                        subjectName = subjectName,
                                        onClick = { onNavigateToDetail(assignment.id) },
                                        onDeleteClick = { viewModel.requestDelete(assignment) },
                                        onStatusClick = { viewModel.cycleStatus(assignment) }
                                    )
                                }
                            }
                        }
                    }

                    state.assignmentToDelete?.let { toDelete ->
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissDeleteDialog() },
                            title = { Text("Confirm Deletion") },
                            text = { Text("Are you sure you want to delete '${toDelete.title}'?") },
                            confirmButton = {
                                Button(onClick = { viewModel.confirmDelete() }) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateAssignmentSimpleDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { subjectId, title, desc, deadline, priority ->
                    viewModel.createAssignment(subjectId, title, desc, deadline, priority)
                }
            )
        }
    }
}

@Composable
private fun CreateAssignmentSimpleDialog(
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, title: String, description: String?, deadline: Long, priority: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Assignment") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val defaultDeadline = System.currentTimeMillis() + 86400000L
                    onConfirm(1L, title, description.ifBlank { null }, defaultDeadline, AssignmentEntity.PRIORITY_HIGH)
                    onDismiss()
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
