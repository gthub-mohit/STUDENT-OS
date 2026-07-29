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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.feature.attendance.presentation.state.EditTimetableUiState
import com.studentos.feature.attendance.presentation.viewmodel.EditTimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimetableScreen(
    viewModel: EditTimetableViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var slotToEdit by remember { mutableStateOf<TimetableSlotEntity?>(null) }
    var slotToDelete by remember { mutableStateOf<TimetableSlotEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Timetable") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    slotToEdit = null
                    showBottomSheet = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Class Slot")
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
                is EditTimetableUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is EditTimetableUiState.Success -> {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val currentDaySlots = state.slots.filter { it.dayOfWeek == state.selectedDayOfWeek }

                    Column(modifier = Modifier.fillMaxSize()) {
                        ScrollableTabRow(
                            selectedTabIndex = (state.selectedDayOfWeek - 1).coerceIn(0, 6),
                            edgePadding = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            days.forEachIndexed { index, dayName ->
                                val dayNum = index + 1
                                Tab(
                                    selected = state.selectedDayOfWeek == dayNum,
                                    onClick = { viewModel.selectDay(dayNum) },
                                    text = { Text(dayName) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (currentDaySlots.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No classes added yet. Tap + to add your first class slot.",
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
                                items(currentDaySlots) { slot ->
                                    val subjectName = state.subjects.find { it.id == slot.subjectId }?.name ?: "Subject #${slot.subjectId}"
                                    TimetableSlotCard(
                                        slot = slot,
                                        subjectName = subjectName,
                                        onEdit = {
                                            slotToEdit = slot
                                            showBottomSheet = true
                                        },
                                        onDelete = {
                                            slotToDelete = slot
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Slot Form Bottom Sheet
                    if (showBottomSheet) {
                        SlotFormBottomSheet(
                            slot = slotToEdit,
                            subjects = state.subjects,
                            selectedDayOfWeek = state.selectedDayOfWeek,
                            onDismiss = { showBottomSheet = false },
                            onSave = { subjectId, day, start, end, room, parity ->
                                if (slotToEdit == null) {
                                    viewModel.addSlot(
                                        subjectId = subjectId,
                                        dayOfWeek = day,
                                        startTime = start,
                                        endTime = end,
                                        location = room.ifBlank { null },
                                        weekParity = parity.ifBlank { null }
                                    )
                                } else {
                                    val updated = slotToEdit!!.copy(
                                        subjectId = subjectId,
                                        dayOfWeek = day,
                                        startTime = start,
                                        endTime = end,
                                        location = room.ifBlank { null },
                                        weekParity = parity.ifBlank { null }
                                    )
                                    viewModel.updateSlot(updated)
                                }
                                showBottomSheet = false
                            }
                        )
                    }

                    // Delete Confirmation Dialog
                    if (slotToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { slotToDelete = null },
                            title = { Text("Delete Class Slot?") },
                            text = { Text("Are you sure you want to delete this class slot?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.deleteSlot(slotToDelete!!.id)
                                        slotToDelete = null
                                    }
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { slotToDelete = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                is EditTimetableUiState.Error -> {
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
private fun TimetableSlotCard(
    slot: TimetableSlotEntity,
    subjectName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    text = subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${slot.startTime} - ${slot.endTime}" + if (slot.location != null) " • Room ${slot.location}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (slot.weekParity != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Parity: ${slot.weekParity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Slot")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Slot", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotFormBottomSheet(
    slot: TimetableSlotEntity?,
    subjects: List<SubjectEntity>,
    selectedDayOfWeek: Int,
    onDismiss: () -> Unit,
    onSave: (Long, Int, String, String, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedSubjectId by remember { mutableStateOf(slot?.subjectId ?: subjects.firstOrNull()?.id ?: 0L) }
    var startTime by remember { mutableStateOf(slot?.startTime ?: "09:00") }
    var endTime by remember { mutableStateOf(slot?.endTime ?: "10:00") }
    var room by remember { mutableStateOf(slot?.location ?: "") }
    var parity by remember { mutableStateOf(slot?.weekParity ?: "") }

    var expandedSubjectPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = if (slot == null) "Add Class Slot" else "Edit Class Slot",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subject Picker Dropdown
            ExposedDropdownMenuBox(
                expanded = expandedSubjectPicker,
                onExpandedChange = { expandedSubjectPicker = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentSubjectName = subjects.find { it.id == selectedSubjectId }?.name ?: "Select Subject"
                OutlinedTextField(
                    value = currentSubjectName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Subject") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubjectPicker) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedSubjectPicker,
                    onDismissRequest = { expandedSubjectPicker = false }
                ) {
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject.name) },
                            onClick = {
                                selectedSubjectId = subject.id
                                expandedSubjectPicker = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End Time") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Room / Location (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = parity,
                onValueChange = { parity = it },
                label = { Text("Week Parity (e.g. ODD / EVEN or empty)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedSubjectId > 0 && startTime.isNotBlank() && endTime.isNotBlank()) {
                        onSave(selectedSubjectId, selectedDayOfWeek, startTime, endTime, room, parity)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Slot")
            }
        }
    }
}
