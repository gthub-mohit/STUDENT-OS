package com.studentos.feature.assignments.presentation.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.feature.assignments.domain.model.AssignmentFilter
import com.studentos.feature.assignments.domain.model.TaskType
import com.studentos.feature.assignments.presentation.component.AssignmentCard
import com.studentos.feature.assignments.presentation.component.AssignmentFilterTabs
import com.studentos.feature.assignments.presentation.component.PrioritizedAssignmentList
import com.studentos.feature.assignments.presentation.state.AssignmentListUiState
import com.studentos.feature.assignments.presentation.viewmodel.AssignmentListViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentListScreen(
    viewModel: AssignmentListViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var isPrioritizedView by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks & Deadlines") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create Task")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // View Mode Toggle (By Status vs By Deadline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !isPrioritizedView,
                    onClick = { isPrioritizedView = false },
                    label = { Text("By Status") }
                )
                FilterChip(
                    selected = isPrioritizedView,
                    onClick = { isPrioritizedView = true },
                    label = { Text("By Deadline") }
                )
            }

            if (!isPrioritizedView) {
                AssignmentFilterTabs(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { viewModel.selectFilter(it) },
                    selectedType = selectedType,
                    onTypeSelected = { viewModel.selectType(it) }
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
                                    text = "No upcoming deadlines 🎉",
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
                                onStatusClick = { viewModel.cycleStatus(it) }
                            )
                        }
                    } else {
                        if (state.assignments.isEmpty()) {
                            val emptyMessage = when {
                                state.currentFilter == AssignmentFilter.TODAY -> "Nothing due today 🎉"
                                state.currentFilter == AssignmentFilter.PENDING -> "You're all caught up 🎉"
                                state.currentTypeFilter == TaskType.QUIZ -> "No quizzes yet."
                                state.currentTypeFilter == TaskType.LAB_RECORD -> "No lab records yet."
                                state.currentTypeFilter == TaskType.PRACTICAL -> "No practicals yet."
                                else -> "No tasks match these filters."
                            }
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = emptyMessage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
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

                    if (showCreateDialog) {
                        CreateTaskDialog(
                            activeSubjects = state.activeSubjects,
                            onDismiss = { showCreateDialog = false },
                            onConfirm = { subjectId, title, desc, deadline, priority, taskType ->
                                viewModel.createAssignment(subjectId, title, desc, deadline, priority, taskType)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskDialog(
    activeSubjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, title: String, description: String?, deadline: Long, priority: String, taskType: TaskType) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember(activeSubjects) { mutableStateOf(activeSubjects.firstOrNull()) }
    var isSubjectDropdownExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(TaskType.ASSIGNMENT) }
    var selectedPriority by remember { mutableStateOf(AssignmentEntity.PRIORITY_MEDIUM) }

    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    val openDatePicker = {
        val now = LocalDate.now()
        val initDate = selectedDate ?: now
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                errorMessage = null
            },
            initDate.year,
            initDate.monthValue - 1,
            initDate.dayOfMonth
        ).show()
    }

    val openTimePicker = {
        val now = LocalTime.now()
        val initTime = selectedTime ?: now
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime = LocalTime.of(hourOfDay, minute)
                errorMessage = null
            },
            initTime.hour,
            initTime.minute,
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title (Required)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text("Title *") },
                    placeholder = { Text("e.g. Mechanics Homework 3") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Course / Subject (Required)
                if (activeSubjects.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = isSubjectDropdownExpanded,
                        onExpandedChange = { isSubjectDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSubject?.name ?: "Select Course *",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Course *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSubjectDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = isSubjectDropdownExpanded,
                            onDismissRequest = { isSubjectDropdownExpanded = false }
                        ) {
                            activeSubjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject.name) },
                                    onClick = {
                                        selectedSubject = subject
                                        isSubjectDropdownExpanded = false
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }
                }

                // Task Type (Required)
                Column {
                    Text(
                        text = "Task Type *",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TaskType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.displayName) }
                            )
                        }
                    }
                }

                // Due Date & Due Time (Both Required)
                Column {
                    Text(
                        text = "Due Date & Time *",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = openDatePicker,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedDate?.format(dateFormatter) ?: "Select Date",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        OutlinedButton(
                            onClick = openTimePicker,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = selectedTime?.format(timeFormatter) ?: "Select Time",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Priority
                Column {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AssignmentEntity.PRIORITY_LOW to "Low",
                            AssignmentEntity.PRIORITY_MEDIUM to "Medium",
                            AssignmentEntity.PRIORITY_HIGH to "High"
                        ).forEach { (pKey, pLabel) ->
                            FilterChip(
                                selected = selectedPriority == pKey,
                                onClick = { selectedPriority = pKey },
                                label = { Text(pLabel) }
                            )
                        }
                    }
                }

                // Description (Optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Validation Error Banner
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Title is required"
                        return@Button
                    }
                    val subject = selectedSubject
                    if (subject == null && activeSubjects.isNotEmpty()) {
                        errorMessage = "Course is required"
                        return@Button
                    }
                    val date = selectedDate
                    if (date == null) {
                        errorMessage = "Due date is required"
                        return@Button
                    }
                    val time = selectedTime
                    if (time == null) {
                        errorMessage = "Due time is required"
                        return@Button
                    }

                    val localDateTime = LocalDateTime.of(date, time)
                    val deadlineEpoch = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val subjectId = subject?.id ?: 1L

                    onConfirm(
                        subjectId,
                        title.trim(),
                        description.trim().ifBlank { null },
                        deadlineEpoch,
                        selectedPriority,
                        selectedType
                    )
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
