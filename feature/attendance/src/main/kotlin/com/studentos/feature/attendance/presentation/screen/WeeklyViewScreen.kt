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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.studentos.feature.attendance.presentation.component.AddExtraClassDialog
import com.studentos.feature.attendance.presentation.component.AttendancePercentageRow
import com.studentos.feature.attendance.presentation.component.BunkCalculatorWidget
import com.studentos.feature.attendance.presentation.component.ClassEventCard
import com.studentos.feature.attendance.presentation.component.DayColumn
import com.studentos.feature.attendance.presentation.state.WeeklyUiState
import com.studentos.feature.attendance.presentation.viewmodel.WeeklyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyViewScreen(
    viewModel: WeeklyViewModel,
    onNavigateToCalendar: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToEditTimetable: () -> Unit = {},
    onNavigateToOcrPreview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddExtraClassDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance") },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import Timetable") },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToOcrPreview()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Timetable Manually") },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToEditTimetable()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attendance Analytics") },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToAnalytics()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Calendar View") },
                                onClick = {
                                    showOverflowMenu = false
                                    onNavigateToCalendar()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is WeeklyUiState.Success) {
                FloatingActionButton(onClick = { showAddExtraClassDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Log Extra Class")
                }
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
                is WeeklyUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is WeeklyUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Week Navigation Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.previousWeek() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Week")
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = state.weekLabel,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (state.weekOffset != 0) {
                                    IconButton(onClick = { viewModel.resetToCurrentWeek() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reset to Current Week")
                                    }
                                }
                            }

                            IconButton(onClick = { viewModel.nextWeek() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Week")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        DayColumn(
                            selectedDayOfWeek = state.selectedDayOfWeek,
                            onDaySelected = { day -> viewModel.selectDay(day) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (state.dayEvents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No classes scheduled for this day.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(state.dayEvents) { event ->
                                    val subjectName = state.subjects.find { it.id == event.subjectId }?.name ?: "Subject"
                                    val slot = state.timetableSlots.find { it.id == event.timetableSlotId }
                                    ClassEventCard(
                                        event = event,
                                        subjectName = subjectName,
                                        roomLocation = slot?.location,
                                        onStatusSelected = { id, status -> viewModel.updateEventStatus(id, status) }
                                    )
                                }
                            }
                        }
                    }

                    if (showAddExtraClassDialog) {
                        AddExtraClassDialog(
                            subjects = state.subjects,
                            onDismiss = { showAddExtraClassDialog = false },
                            onConfirm = { subjectId, scheduledAt, endAt ->
                                viewModel.addExtraClass(subjectId, scheduledAt, endAt)
                            }
                        )
                    }
                }
                is WeeklyUiState.Error -> {
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
