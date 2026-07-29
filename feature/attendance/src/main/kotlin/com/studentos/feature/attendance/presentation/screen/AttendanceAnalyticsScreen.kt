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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.feature.attendance.domain.calculator.BunkCalculator
import com.studentos.feature.attendance.presentation.component.AttendancePercentageRow
import com.studentos.feature.attendance.presentation.component.BunkCalculatorWidget
import com.studentos.feature.attendance.presentation.state.AnalyticsUiState
import com.studentos.feature.attendance.presentation.viewmodel.AttendanceAnalyticsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceAnalyticsScreen(
    viewModel: AttendanceAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                is AnalyticsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AnalyticsUiState.Success -> {
                    if (state.summaries.isEmpty() || state.totalHeldCount == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No attendance recorded yet. Start marking classes from Weekly View.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            AttendancePercentageRow(
                                percentage = state.overallPercentage,
                                isBelowThreshold = state.isBelowThreshold,
                                threshold = state.threshold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Total Attended: ${state.totalPresentCount} / ${state.totalHeldCount} classes held",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.summaries) { summary ->
                                    SubjectAnalyticsCard(
                                        summary = summary,
                                        threshold = state.threshold
                                    )
                                }
                            }
                        }
                    }
                }
                is AnalyticsUiState.Error -> {
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
private fun SubjectAnalyticsCard(
    summary: SubjectAttendanceSummary,
    threshold: Int,
    modifier: Modifier = Modifier
) {
    val isBelow = summary.percentage < threshold
    val canSkip = BunkCalculator.canSkip(
        present = summary.presentCount,
        absent = summary.absentCount,
        cancelled = summary.cancelledCount,
        holiday = summary.holidayCount,
        extraPresent = summary.extraPresentCount,
        threshold = threshold
    )

    val mustAttend = BunkCalculator.mustAttend(
        present = summary.presentCount,
        absent = summary.absentCount,
        cancelled = summary.cancelledCount,
        holiday = summary.holidayCount,
        extraPresent = summary.extraPresentCount,
        threshold = threshold
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isBelow) Color(0xFFF8D7DA) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isBelow) Color(0xFF721C24) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", summary.percentage),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isBelow) Color(0xFF721C24) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (summary.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (isBelow) Color(0xFF721C24) else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Attended: ${summary.presentCount + summary.extraPresentCount} / ${summary.totalHeldCount} held | Absent: ${summary.absentCount} | Cancelled: ${summary.cancelledCount}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isBelow) Color(0xFF721C24) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            BunkCalculatorWidget(
                canSkip = canSkip,
                mustAttend = mustAttend,
                threshold = threshold
            )
        }
    }
}
