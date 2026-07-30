package com.studentos.feature.assignments.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studentos.feature.assignments.presentation.component.AttachmentRow
import com.studentos.feature.assignments.presentation.component.DeadlineCountdown
import com.studentos.feature.assignments.presentation.component.PriorityBadge
import com.studentos.feature.assignments.presentation.component.StatusChip
import com.studentos.feature.assignments.presentation.state.AssignmentDetailUiState
import com.studentos.feature.assignments.presentation.viewmodel.AssignmentDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDetailScreen(
    viewModel: AssignmentDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    val title = (uiState as? AssignmentDetailUiState.Success)?.assignment?.title ?: "Assignment Detail"
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
                            Icon(Icons.Default.Delete, contentDescription = "Delete Assignment")
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

                    if (state.showDeleteConfirmation) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissDeleteDialog() },
                            title = { Text("Delete Assignment") },
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
