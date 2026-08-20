package com.studentos.feature.projects.presentation.component

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.domain.model.ProjectTaskPriority
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    taskToEdit: ProjectTaskDomain?,
    allProjectTasks: List<ProjectTaskDomain>,
    dialogError: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dependencyTaskId: Long?, priority: ProjectTaskPriority, deadline: Long?) -> Unit
) {
    val context = LocalContext.current
    var title by remember(taskToEdit) { mutableStateOf(taskToEdit?.title ?: "") }
    var selectedDependencyId by remember(taskToEdit) { mutableStateOf(taskToEdit?.dependencyTaskId) }
    var selectedPriority by remember(taskToEdit) { mutableStateOf(taskToEdit?.priority ?: ProjectTaskPriority.MEDIUM) }
    var selectedDeadline by remember(taskToEdit) { mutableStateOf(taskToEdit?.deadline) }

    var titleError by remember { mutableStateOf(false) }
    var isDependencyDropdownExpanded by remember { mutableStateOf(false) }

    // Candidates are all tasks in project except this task itself
    val candidates = remember(allProjectTasks, taskToEdit) {
        allProjectTasks.filter { it.id != (taskToEdit?.id ?: 0L) }
    }

    val selectedDepTask = remember(selectedDependencyId, candidates) {
        candidates.firstOrNull { it.id == selectedDependencyId }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (taskToEdit == null) "New Project Task" else "Edit Project Task",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Task Title
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            titleError = it.isBlank()
                        },
                        label = { Text("Task Title") },
                        isError = titleError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (titleError) {
                        Text(
                            text = "Task title cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // 2. Dependency Selector ("Depends on")
                ExposedDropdownMenuBox(
                    expanded = isDependencyDropdownExpanded,
                    onExpandedChange = { isDependencyDropdownExpanded = !isDependencyDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDepTask?.title ?: "None (Independent)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Depends on (Prerequisite)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDependencyDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isDependencyDropdownExpanded,
                        onDismissRequest = { isDependencyDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Independent)") },
                            onClick = {
                                selectedDependencyId = null
                                isDependencyDropdownExpanded = false
                            }
                        )
                        candidates.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(candidate.title) },
                                onClick = {
                                    selectedDependencyId = candidate.id
                                    isDependencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 3. Priority Selector
                Column {
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedPriority == ProjectTaskPriority.LOW,
                            onClick = { selectedPriority = ProjectTaskPriority.LOW },
                            label = { Text("Low") }
                        )
                        FilterChip(
                            selected = selectedPriority == ProjectTaskPriority.MEDIUM,
                            onClick = { selectedPriority = ProjectTaskPriority.MEDIUM },
                            label = { Text("Medium") }
                        )
                        FilterChip(
                            selected = selectedPriority == ProjectTaskPriority.HIGH,
                            onClick = { selectedPriority = ProjectTaskPriority.HIGH },
                            label = { Text("High") }
                        )
                    }
                }

                // 4. Deadline Selector
                Column {
                    Text(
                        text = "Deadline (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val deadlineText = selectedDeadline?.let {
                            val localDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            localDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        } ?: "No deadline set"

                        Text(
                            text = deadlineText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedDeadline != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (selectedDeadline != null) {
                                TextButton(onClick = { selectedDeadline = null }) {
                                    Text("Clear")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    selectedDeadline?.let { calendar.timeInMillis = it }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                                            val instant = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                                            selectedDeadline = instant.toEpochMilli()
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            ) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pick Date")
                            }
                        }
                    }
                }

                // 5. Validation error display
                if (dialogError != null) {
                    Text(
                        text = dialogError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onConfirm(title, selectedDependencyId, selectedPriority, selectedDeadline)
                    }
                }
            ) {
                Text(text = if (taskToEdit == null) "Add Task" else "Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}
