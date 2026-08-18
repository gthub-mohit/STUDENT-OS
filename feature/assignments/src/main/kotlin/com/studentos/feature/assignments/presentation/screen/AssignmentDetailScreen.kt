package com.studentos.feature.assignments.presentation.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.studentos.feature.assignments.domain.model.TaskType
import com.studentos.feature.assignments.presentation.component.AttachmentRow
import com.studentos.feature.assignments.presentation.component.DeadlineCountdown
import com.studentos.feature.assignments.presentation.component.PriorityBadge
import com.studentos.feature.assignments.presentation.component.StatusChip
import com.studentos.feature.assignments.presentation.component.TaskTypeBadge
import com.studentos.feature.assignments.presentation.state.AssignmentDetailUiState
import com.studentos.feature.assignments.presentation.viewmodel.AssignmentDetailViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailScreen(
    viewModel: AssignmentDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showEditDeadlineDialog by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addAttachment(it.toString()) }
    }

    LaunchedEffect(uiState) {
        if (uiState is AssignmentDetailUiState.Deleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? AssignmentDetailUiState.Success)?.assignment?.title ?: "Task Detail"
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is AssignmentDetailUiState.Success) {
                        IconButton(onClick = { viewModel.requestDelete() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Task")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is AssignmentDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AssignmentDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is AssignmentDetailUiState.Deleted -> {
                    // Handled in LaunchedEffect
                }
                is AssignmentDetailUiState.Success -> {
                    val assignment = state.assignment
                    val taskType = TaskType.fromString(assignment.taskType)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.subjectName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TaskTypeBadge(taskType = taskType)
                            Spacer(modifier = Modifier.width(6.dp))
                            PriorityBadge(priority = assignment.priority)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DeadlineCountdown(
                                deadline = assignment.deadline,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showEditDeadlineDialog = true }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Deadline",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            StatusChip(
                                status = assignment.status,
                                onClick = { viewModel.cycleStatus() }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val desc = assignment.description.orEmpty()
                                Text(
                                    text = desc.ifEmpty { "No description provided." },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Attachment",
                            style = MaterialTheme.typography.titleSmall
                        )
                        AttachmentRow(
                            attachmentUri = assignment.attachmentUri,
                            onAddAttachment = { filePickerLauncher.launch("*/*") },
                            onReplaceAttachment = { filePickerLauncher.launch("*/*") },
                            onRemoveAttachment = { viewModel.removeAttachment() }
                        )
                    }

                    if (showEditDeadlineDialog) {
                        val currentZoned = Instant.ofEpochMilli(assignment.deadline).atZone(ZoneId.systemDefault())
                        var editDate by remember { mutableStateOf(currentZoned.toLocalDate()) }
                        var editTime by remember { mutableStateOf(currentZoned.toLocalTime()) }
                        val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
                        val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

                        AlertDialog(
                            onDismissRequest = { showEditDeadlineDialog = false },
                            title = { Text("Edit Due Date & Time") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    editDate = LocalDate.of(year, month + 1, dayOfMonth)
                                                },
                                                editDate.year,
                                                editDate.monthValue - 1,
                                                editDate.dayOfMonth
                                            ).show()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Date: ${editDate.format(dateFormatter)}")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, hourOfDay, minute ->
                                                    editTime = LocalTime.of(hourOfDay, minute)
                                                },
                                                editTime.hour,
                                                editTime.minute,
                                                false
                                            ).show()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Time: ${editTime.format(timeFormatter)}")
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val newLocalDateTime = LocalDateTime.of(editDate, editTime)
                                    val newDeadlineEpoch = newLocalDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    viewModel.updateDeadline(newDeadlineEpoch)
                                    showEditDeadlineDialog = false
                                }) {
                                    Text("Save")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showEditDeadlineDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (state.showDeleteConfirmation) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissDeleteDialog() },
                            title = { Text("Delete Task") },
                            text = { Text("Are you sure you want to delete '${assignment.title}'?") },
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
    }
}
