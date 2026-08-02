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
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.presentation.component.CreateTaskDialog
import com.studentos.feature.projects.presentation.component.TaskItem
import com.studentos.feature.projects.presentation.state.ProjectTaskUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectTaskScreen(
    uiState: ProjectTaskUiState,
    onBackClick: () -> Unit,
    onModeToggle: (Boolean) -> Unit,
    onCreateTaskClick: () -> Unit,
    onEditTaskClick: (ProjectTaskDomain) -> Unit,
    onToggleTaskCompletion: (ProjectTaskDomain) -> Unit,
    onSetNextAction: (Long) -> Unit,
    onDeleteTaskClick: (Long) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDialog: (title: String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.project?.title ?: "Project Details") },
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
            FloatingActionButton(onClick = onCreateTaskClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !uiState.isParallelMode,
                        onClick = { onModeToggle(false) },
                        label = { Text("Sequential") }
                    )
                    FilterChip(
                        selected = uiState.isParallelMode,
                        onClick = { onModeToggle(true) },
                        label = { Text("Parallel") }
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
                                text = "Loading Tasks...",
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
                                text = "No tasks yet.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap + to add your first task to this project.",
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (uiState.pendingTasks.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Pending Tasks (${uiState.pendingTasks.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(
                                    items = uiState.pendingTasks,
                                    key = { it.id }
                                ) { task ->
                                    TaskItem(
                                        task = task,
                                        onToggleCompletion = { onToggleTaskCompletion(task) },
                                        onSetNextAction = { onSetNextAction(task.id) },
                                        onEditClick = { onEditTaskClick(task) },
                                        onDeleteClick = { onDeleteTaskClick(task.id) }
                                    )
                                }
                            }

                            if (uiState.completedTasks.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Completed (${uiState.completedTasks.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(
                                    items = uiState.completedTasks,
                                    key = { it.id }
                                ) { task ->
                                    TaskItem(
                                        task = task,
                                        onToggleCompletion = { onToggleTaskCompletion(task) },
                                        onSetNextAction = {},
                                        onEditClick = { onEditTaskClick(task) },
                                        onDeleteClick = { onDeleteTaskClick(task.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isCreateTaskDialogOpen) {
            CreateTaskDialog(
                taskToEdit = uiState.taskToEdit,
                onDismiss = onDismissDialog,
                onConfirm = onConfirmDialog
            )
        }
    }
}
