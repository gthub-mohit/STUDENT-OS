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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentos.feature.projects.presentation.component.CreateProjectDialog
import com.studentos.feature.projects.presentation.component.ProjectCard
import com.studentos.feature.projects.presentation.state.ProjectsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    uiState: ProjectsUiState,
    currentTimeMs: Long,
    onTabSelected: (Boolean) -> Unit,
    onCreateClick: () -> Unit,
    onEditClick: (com.studentos.feature.projects.domain.model.ProjectDomain) -> Unit,
    onArchiveClick: (Long) -> Unit,
    onUnarchiveClick: (Long) -> Unit,
    onProjectClick: (Long) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDialog: (title: String, inactivityThresholdDays: Int) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Projects") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Project")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !uiState.showArchivedTab,
                    onClick = { onTabSelected(false) },
                    label = { Text("Active (${uiState.activeProjects.size})") }
                )
                FilterChip(
                    selected = uiState.showArchivedTab,
                    onClick = { onTabSelected(true) },
                    label = { Text("Archived (${uiState.archivedProjects.size})") }
                )
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
                                text = "Loading Projects...",
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
                                text = if (uiState.showArchivedTab) "No archived projects." else "No active projects.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.showArchivedTab) "Archived projects will appear here." else "Tap + to create your first project.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.displayedProjects,
                                key = { it.id }
                            ) { project ->
                                ProjectCard(
                                    project = project,
                                    currentTimeMs = currentTimeMs,
                                    onClick = { onProjectClick(project.id) },
                                    onEditClick = { onEditClick(project) },
                                    onArchiveClick = {
                                        if (project.isArchived) {
                                            onUnarchiveClick(project.id)
                                        } else {
                                            onArchiveClick(project.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isCreateDialogOpen) {
            CreateProjectDialog(
                projectToEdit = uiState.projectToEdit,
                onDismiss = onDismissDialog,
                onConfirm = onConfirmDialog
            )
        }
    }
}
