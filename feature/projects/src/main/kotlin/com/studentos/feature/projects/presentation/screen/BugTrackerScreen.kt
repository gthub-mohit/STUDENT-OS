package com.studentos.feature.projects.presentation.screen

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentos.feature.projects.domain.model.BugDomain
import com.studentos.feature.projects.presentation.component.BugCard
import com.studentos.feature.projects.presentation.component.CreateBugDialog
import com.studentos.feature.projects.presentation.state.BugSeverityFilter
import com.studentos.feature.projects.presentation.state.BugStatusFilter
import com.studentos.feature.projects.presentation.state.BugTrackerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugTrackerScreen(
    uiState: BugTrackerUiState,
    onBackClick: () -> Unit,
    onStatusFilterSelected: (BugStatusFilter) -> Unit,
    onSeverityFilterSelected: (BugSeverityFilter) -> Unit,
    onCreateBugClick: () -> Unit,
    onEditBugClick: (BugDomain) -> Unit,
    onToggleBugResolution: (BugDomain) -> Unit,
    onDeleteBugClick: (Long) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDialog: (description: String, severity: String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "${uiState.project?.title ?: "Project"} Issue Tracker") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateBugClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Report Bug")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Status Filter",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.statusFilter == BugStatusFilter.OPEN,
                        onClick = { onStatusFilterSelected(BugStatusFilter.OPEN) },
                        label = { Text("Open (${uiState.openCount})") }
                    )
                    FilterChip(
                        selected = uiState.statusFilter == BugStatusFilter.RESOLVED,
                        onClick = { onStatusFilterSelected(BugStatusFilter.RESOLVED) },
                        label = { Text("Resolved (${uiState.resolvedCount})") }
                    )
                    FilterChip(
                        selected = uiState.statusFilter == BugStatusFilter.ALL,
                        onClick = { onStatusFilterSelected(BugStatusFilter.ALL) },
                        label = { Text("All (${uiState.bugs.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Severity Filter",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.severityFilter == BugSeverityFilter.ALL,
                        onClick = { onSeverityFilterSelected(BugSeverityFilter.ALL) },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = uiState.severityFilter == BugSeverityFilter.HIGH,
                        onClick = { onSeverityFilterSelected(BugSeverityFilter.HIGH) },
                        label = { Text("High") }
                    )
                    FilterChip(
                        selected = uiState.severityFilter == BugSeverityFilter.MEDIUM,
                        onClick = { onSeverityFilterSelected(BugSeverityFilter.MEDIUM) },
                        label = { Text("Medium") }
                    )
                    FilterChip(
                        selected = uiState.severityFilter == BugSeverityFilter.LOW,
                        onClick = { onSeverityFilterSelected(BugSeverityFilter.LOW) },
                        label = { Text("Low") }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading Issues...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    uiState.errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetryClick) {
                                Text(text = "Retry")
                            }
                        }
                    }

                    uiState.isEmpty -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No issues match the selected filter.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to report a new bug or issue for this project.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = uiState.filteredBugs,
                                key = { it.id }
                            ) { bug ->
                                BugCard(
                                    bug = bug,
                                    onToggleResolution = { onToggleBugResolution(bug) },
                                    onEditClick = { onEditBugClick(bug) },
                                    onDeleteClick = { onDeleteBugClick(bug.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isCreateDialogOpen) {
            CreateBugDialog(
                bugToEdit = uiState.bugToEdit,
                onDismiss = onDismissDialog,
                onConfirm = onConfirmDialog
            )
        }
    }
}
