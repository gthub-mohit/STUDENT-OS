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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentos.feature.projects.domain.model.NextActionStatus
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.domain.model.ProjectTaskEngine
import com.studentos.feature.projects.domain.model.ProjectTaskPriority
import com.studentos.feature.projects.presentation.component.CreateTaskDialog
import com.studentos.feature.projects.presentation.component.ProjectProgressBar
import com.studentos.feature.projects.presentation.component.ProjectTaskFilterBottomSheet
import com.studentos.feature.projects.presentation.component.TaskItem
import com.studentos.feature.projects.presentation.state.ProjectTaskStatusFilter
import com.studentos.feature.projects.presentation.state.ProjectTaskUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectTaskScreen(
    uiState: ProjectTaskUiState,
    onBackClick: () -> Unit,
    onCreateTaskClick: () -> Unit,
    onEditTaskClick: (ProjectTaskDomain) -> Unit,
    onToggleTaskCompletion: (ProjectTaskDomain) -> Unit,
    onDeleteTaskClick: (Long) -> Unit,
    onFilterClick: () -> Unit,
    onFilterDismiss: () -> Unit,
    onFilterApply: (ProjectTaskStatusFilter) -> Unit,
    onClearFilters: () -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDialog: (title: String, dependencyTaskId: Long?, priority: ProjectTaskPriority, deadline: Long?) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTasks = uiState.tasks.size
    val completedCount = uiState.completedTasks.size
    val progressFraction = if (totalTasks > 0) completedCount.toFloat() / totalTasks.toFloat() else 0f
    val progressPct = (progressFraction * 100f).toInt()
    val nextActionRec = uiState.nextAction

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.project?.title ?: "Project Tasks") },
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
            if (uiState.tasks.isNotEmpty()) {
                FloatingActionButton(onClick = onCreateTaskClick) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading Tasks...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
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
                }

                uiState.hasNoTasksInProject -> {
                    // 0 tasks total in DB: Hide filters, hide FAB, show clean empty state + prominent Add Task CTA
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No tasks yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add your first task to get started.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onCreateTaskClick) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Add Task")
                            }
                        }
                    }
                }

                else -> {
                    // Tasks exist in project: Show Progress Card, Next Action Card, and Filters Control
                    // 1. Progress Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Progress: $progressPct%",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$completedCount/$totalTasks tasks",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            ProjectProgressBar(
                                progress = progressFraction,
                                height = 6.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }
                    }

                    // 2. Intelligent Next Action Card
                    if (nextActionRec.status != NextActionStatus.NONE) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (nextActionRec.status) {
                                    NextActionStatus.AVAILABLE_TASK -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    NextActionStatus.WAITING_ON_DEPENDENCIES -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                    NextActionStatus.PROJECT_COMPLETE -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                    NextActionStatus.NONE -> MaterialTheme.colorScheme.surfaceContainerLow
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                when (nextActionRec.status) {
                                    NextActionStatus.AVAILABLE_TASK -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "NEXT ACTION",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (nextActionRec.deadlineContext != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                                ) {
                                                    Text(
                                                        text = nextActionRec.deadlineContext,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = nextActionRec.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = nextActionRec.reason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    NextActionStatus.WAITING_ON_DEPENDENCIES -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Waiting on dependencies",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = nextActionRec.reason,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    NextActionStatus.PROJECT_COMPLETE -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Project complete",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = nextActionRec.reason,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    NextActionStatus.NONE -> {}
                                }
                            }
                        }
                    }

                    // 3. Summary & Filters Row (Always visible when tasks exist in DB)
                    val count = uiState.filteredTasks.size
                    val countLabel = if (count == 1) "1 task" else "$count tasks"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = onFilterClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(text = uiState.filterButtonLabel)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Filters",
                                modifier = Modifier.height(18.dp)
                            )
                        }
                    }

                    // 4. Categorized Task Lists or Filtered Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        if (uiState.hasNoFilteredResults) {
                            // Tasks exist in project, but active filter produced 0 results
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No tasks match these filters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Try adjusting or clearing your filters.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(onClick = onClearFilters) {
                                    Text(text = "Clear Filters")
                                }
                            }
                        } else {
                            val map = uiState.tasksMap
                            val displayAvailable = uiState.filteredTasks.filter {
                                !it.isCompleted && ProjectTaskEngine.getTaskState(it, map) == com.studentos.feature.projects.domain.model.ProjectTaskState.AVAILABLE
                            }
                            val displayBlocked = uiState.filteredTasks.filter {
                                !it.isCompleted && ProjectTaskEngine.getTaskState(it, map) == com.studentos.feature.projects.domain.model.ProjectTaskState.BLOCKED
                            }
                            val displayCompleted = uiState.filteredTasks.filter { it.isCompleted }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (displayAvailable.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "AVAILABLE (${displayAvailable.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(
                                        items = displayAvailable,
                                        key = { it.id }
                                    ) { task ->
                                        TaskItem(
                                            task = task,
                                            taskState = com.studentos.feature.projects.domain.model.ProjectTaskState.AVAILABLE,
                                            blockerTask = null,
                                            onToggleCompletion = { onToggleTaskCompletion(task) },
                                            onEditClick = { onEditTaskClick(task) },
                                            onDeleteClick = { onDeleteTaskClick(task.id) },
                                            nowMs = uiState.currentTimeMs
                                        )
                                    }
                                }

                                if (displayBlocked.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "BLOCKED (${displayBlocked.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(
                                        items = displayBlocked,
                                        key = { it.id }
                                    ) { task ->
                                        val blocker = ProjectTaskEngine.getBlockerTask(task, map)
                                        TaskItem(
                                            task = task,
                                            taskState = com.studentos.feature.projects.domain.model.ProjectTaskState.BLOCKED,
                                            blockerTask = blocker,
                                            onToggleCompletion = { onToggleTaskCompletion(task) },
                                            onEditClick = { onEditTaskClick(task) },
                                            onDeleteClick = { onDeleteTaskClick(task.id) },
                                            nowMs = uiState.currentTimeMs
                                        )
                                    }
                                }

                                if (displayCompleted.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "COMPLETED (${displayCompleted.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    items(
                                        items = displayCompleted,
                                        key = { it.id }
                                    ) { task ->
                                        TaskItem(
                                            task = task,
                                            taskState = com.studentos.feature.projects.domain.model.ProjectTaskState.COMPLETED,
                                            blockerTask = null,
                                            onToggleCompletion = { onToggleTaskCompletion(task) },
                                            onEditClick = { onEditTaskClick(task) },
                                            onDeleteClick = { onDeleteTaskClick(task.id) },
                                            nowMs = uiState.currentTimeMs
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isFilterSheetOpen) {
            ProjectTaskFilterBottomSheet(
                currentFilter = uiState.statusFilter,
                onDismiss = onFilterDismiss,
                onApply = onFilterApply,
                onClearAll = onClearFilters
            )
        }

        if (uiState.isCreateTaskDialogOpen) {
            CreateTaskDialog(
                taskToEdit = uiState.taskToEdit,
                allProjectTasks = uiState.tasks,
                dialogError = uiState.dialogError,
                onDismiss = onDismissDialog,
                onConfirm = onConfirmDialog
            )
        }
    }
}
