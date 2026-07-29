package com.studentos.feature.attendance.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.feature.attendance.presentation.state.ManageSubjectsUiState
import com.studentos.feature.attendance.presentation.viewmodel.ManageSubjectsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubjectsScreen(
    viewModel: ManageSubjectsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToRename by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToArchive by remember { mutableStateOf<SubjectEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Subjects") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ManageSubjectsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ManageSubjectsUiState.Success -> {
                    if (state.activeSubjects.isEmpty() && state.archivedSubjects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No subjects registered yet. Tap + to add your first subject.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (state.activeSubjects.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Active Subjects",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(state.activeSubjects) { subject ->
                                    SubjectItemCard(
                                        subject = subject,
                                        isArchived = false,
                                        onRename = { subjectToRename = subject },
                                        onArchive = { subjectToArchive = subject }
                                    )
                                }
                            }

                            if (state.archivedSubjects.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Archived Subjects (Read-Only Analytics)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(state.archivedSubjects) { subject ->
                                    SubjectItemCard(
                                        subject = subject,
                                        isArchived = true,
                                        onRename = {},
                                        onArchive = {}
                                    )
                                }
                            }
                        }
                    }

                    // Add Subject Dialog
                    if (showAddDialog) {
                        var newSubjectName by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showAddDialog = false },
                            title = { Text("Add New Subject") },
                            text = {
                                OutlinedTextField(
                                    value = newSubjectName,
                                    onValueChange = { newSubjectName = it },
                                    label = { Text("Subject Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (newSubjectName.isNotBlank()) {
                                            viewModel.addSubject(newSubjectName)
                                            showAddDialog = false
                                        }
                                    }
                                ) {
                                    Text("Add")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Rename Subject Dialog
                    if (subjectToRename != null) {
                        var updatedName by remember { mutableStateOf(subjectToRename!!.name) }
                        AlertDialog(
                            onDismissRequest = { subjectToRename = null },
                            title = { Text("Rename Subject") },
                            text = {
                                OutlinedTextField(
                                    value = updatedName,
                                    onValueChange = { updatedName = it },
                                    label = { Text("New Subject Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (updatedName.isNotBlank()) {
                                            viewModel.renameSubject(subjectToRename!!.id, updatedName)
                                            subjectToRename = null
                                        }
                                    }
                                ) {
                                    Text("Rename")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { subjectToRename = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Archive Subject Dialog
                    if (subjectToArchive != null) {
                        AlertDialog(
                            onDismissRequest = { subjectToArchive = null },
                            title = { Text("Archive Subject?") },
                            text = {
                                Text("Archiving this subject will remove it from active weekly schedules. Historical attendance logs and class events will be preserved in read-only analytics.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.archiveSubject(subjectToArchive!!.id, confirmWithActiveSlots = true)
                                        subjectToArchive = null
                                    }
                                ) {
                                    Text("Archive")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { subjectToArchive = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                is ManageSubjectsUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectItemCard(
    subject: SubjectEntity,
    isArchived: Boolean,
    onRename: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isArchived) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isArchived) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isArchived) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "Archived",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (!isArchived) {
                Row {
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename Subject")
                    }
                    IconButton(onClick = onArchive) {
                        Icon(Icons.Default.Delete, contentDescription = "Archive Subject", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
