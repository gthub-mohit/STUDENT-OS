package com.studentos.feature.attendance.presentation.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import com.studentos.feature.attendance.presentation.viewmodel.OcrUiState
import com.studentos.feature.attendance.presentation.viewmodel.OcrViewModel

private val AmberHighlight = Color(0xFFFFF3CD)
private val AmberText = Color(0xFF856404)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrPreviewScreen(
    viewModel: OcrViewModel,
    onImportFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Timetable") }
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
                is OcrUiState.Idle -> {
                    Text(
                        text = "No image processed",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is OcrUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is OcrUiState.Content -> {
                    OcrContent(
                        state = state,
                        onUpdateSlot = { index, slot -> viewModel.updateSlot(index, slot) },
                        onRemoveSlot = { index -> viewModel.removeSlot(index) },
                        onConfirmImport = { viewModel.confirmImport(replaceExisting = false) }
                    )

                    if (state.showReplaceDialog) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissReplaceDialog() },
                            title = { Text("Replace Timetable?") },
                            text = { Text("An existing timetable already exists. Overwrite all existing classes?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.dismissReplaceDialog()
                                        viewModel.confirmImport(replaceExisting = true)
                                    }
                                ) {
                                    Text("Replace")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { viewModel.dismissReplaceDialog() }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                is OcrUiState.ImportSuccess -> {
                    onImportFinished()
                }
                is OcrUiState.Error -> {
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
private fun OcrContent(
    state: OcrUiState.Content,
    onUpdateSlot: (Int, ParsedTimetableSlot) -> Unit,
    onRemoveSlot: (Int) -> Unit,
    onConfirmImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (state.hasWarnings) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AmberHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Some fields have low OCR confidence (<80%) and are highlighted in amber. Please verify.",
                    color = AmberText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(state.slots) { index, slot ->
                SlotRowItem(
                    slot = slot,
                    onSlotChange = { updated -> onUpdateSlot(index, updated) },
                    onDelete = { onRemoveSlot(index) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onConfirmImport,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.slots.isNotEmpty()
        ) {
            Text("Confirm Import")
        }
    }
}

@Composable
private fun SlotRowItem(
    slot: ParsedTimetableSlot,
    onSlotChange: (ParsedTimetableSlot) -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor = if (slot.isLowConfidence) AmberHighlight else MaterialTheme.colorScheme.surfaceVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getDayName(slot.dayOfWeek),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onDelete) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = slot.subjectName,
                onValueChange = { onSlotChange(slot.copy(subjectName = it)) },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = slot.startTime,
                    onValueChange = { onSlotChange(slot.copy(startTime = it)) },
                    label = { Text("Start Time") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = slot.endTime,
                    onValueChange = { onSlotChange(slot.copy(endTime = it)) },
                    label = { Text("End Time") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = slot.location ?: "",
                onValueChange = { onSlotChange(slot.copy(location = it.ifBlank { null })) },
                label = { Text("Location (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun getDayName(dayOfWeek: Int): String {
    return when (dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Monday"
    }
}
