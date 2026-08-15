package com.studentos.feature.attendance.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.feature.attendance.domain.calculator.BunkCalculator
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
                title = { Text("Attendance Analytics & Prediction") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        when (val state = uiState) {
            is AnalyticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AnalyticsUiState.Success -> {
                if (state.summaries.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No Subjects or Timetable Found",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import your timetable or add subjects to see subject-wise attendance analytics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go to Weekly View")
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (state.totalHeldCount > 0) {
                            item {
                                OverallSummaryHeader(
                                    overallPercentage = state.overallPercentage,
                                    totalPresent = state.totalPresentCount,
                                    totalHeld = state.totalHeldCount,
                                    threshold = state.threshold,
                                    isBelowThreshold = state.isBelowThreshold
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Subject Attendance & Prediction",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        items(state.summaries, key = { it.subjectId }) { summary ->
                            SubjectAnalyticsCard(
                                summary = summary,
                                threshold = state.threshold
                            )
                        }
                    }
                }
            }
            is AnalyticsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallSummaryHeader(
    overallPercentage: Double,
    totalPresent: Int,
    totalHeld: Int,
    threshold: Int,
    isBelowThreshold: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isBelowThreshold) Color(0xFFF8D7DA) else MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overall Attendance",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isBelowThreshold) Color(0xFF721C24) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", overallPercentage),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isBelowThreshold) Color(0xFF721C24) else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$totalPresent / $totalHeld attended",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isBelowThreshold) Color(0xFF721C24) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = "Target: $threshold%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isBelowThreshold) Color(0xFF721C24) else MaterialTheme.colorScheme.onPrimaryContainer
                )
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
    var isExpanded by remember { mutableStateOf(false) }
    var futureAttended by remember { mutableIntStateOf(0) }
    var futureBunked by remember { mutableIntStateOf(0) }

    val hasData = summary.totalHeldCount > 0
    val isBelow = hasData && (summary.percentage < threshold)
    val isCritical = hasData && (summary.percentage < threshold - 15.0)

    val canSkip = if (hasData) {
        BunkCalculator.canSkip(
            present = summary.presentCount,
            absent = summary.absentCount,
            cancelled = summary.cancelledCount,
            holiday = summary.holidayCount,
            extraPresent = summary.extraPresentCount,
            threshold = threshold
        )
    } else 0

    val mustAttend = if (hasData) {
        BunkCalculator.mustAttend(
            present = summary.presentCount,
            absent = summary.absentCount,
            cancelled = summary.cancelledCount,
            holiday = summary.holidayCount,
            extraPresent = summary.extraPresentCount,
            threshold = threshold
        )
    } else 0

    val cardBorder = when {
        !hasData -> null
        isCritical -> BorderStroke(1.5.dp, Color(0xFFDC3545))
        isBelow -> BorderStroke(1.dp, Color(0xFFFFC107))
        else -> null
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = cardBorder,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Subject Name + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasData) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", summary.percentage),
                        style = MaterialTheme.typography.titleLarge,
                        color = when {
                            isCritical -> Color(0xFF721C24)
                            isBelow -> Color(0xFF856404)
                            else -> Color(0xFF155724)
                        }
                    )
                } else {
                    Text(
                        text = "No data yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress Bar
            if (hasData) {
                LinearProgressIndicator(
                    progress = { (summary.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = when {
                        isCritical -> Color(0xFFDC3545)
                        isBelow -> Color(0xFFFFC107)
                        else -> Color(0xFF28A745)
                    },
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Attendance Counts
            if (hasData) {
                Text(
                    text = "${summary.presentCount + summary.extraPresentCount} / ${summary.totalHeldCount} attended • ${summary.absentCount} absent • ${summary.cancelledCount} cancelled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No attendance recorded yet for this subject.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status Pill + View Prediction Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Pill
                val statusText = when {
                    !hasData -> "Target: $threshold%"
                    canSkip > 0 -> "Safe — Can skip $canSkip ${if (canSkip == 1) "class" else "classes"}"
                    mustAttend > 0 -> if (isCritical) {
                        "Critical — Must attend $mustAttend ${if (mustAttend == 1) "class" else "classes"}"
                    } else {
                        "Warning — Must attend $mustAttend ${if (mustAttend == 1) "class" else "classes"}"
                    }
                    else -> "On Track (Safe)"
                }

                val (badgeBg, badgeText) = when {
                    !hasData -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurfaceVariant
                    isCritical -> Color(0xFFF8D7DA) to Color(0xFF721C24)
                    isBelow -> Color(0xFFFFF3CD) to Color(0xFF856404)
                    else -> Color(0xFFD4EDDA) to Color(0xFF155724)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = statusText,
                        color = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "Hide Prediction" else "View Prediction")
                }
            }

            // Expandable Subject Prediction Panel
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                SubjectPredictionSection(
                    summary = summary,
                    threshold = threshold,
                    futureAttended = futureAttended,
                    futureBunked = futureBunked,
                    onFutureAttendedChange = { futureAttended = it },
                    onFutureBunkedChange = { futureBunked = it }
                )
            }
        }
    }
}

@Composable
private fun SubjectPredictionSection(
    summary: SubjectAttendanceSummary,
    threshold: Int,
    futureAttended: Int,
    futureBunked: Int,
    onFutureAttendedChange: (Int) -> Unit,
    onFutureBunkedChange: (Int) -> Unit
) {
    val predictedPct = BunkCalculator.predictAttendance(
        present = summary.presentCount,
        absent = summary.absentCount,
        cancelled = summary.cancelledCount,
        holiday = summary.holidayCount,
        extraPresent = summary.extraPresentCount,
        futureAttended = futureAttended,
        futureBunked = futureBunked
    )

    val isPredictedSafe = predictedPct >= threshold
    val isPredictedCritical = predictedPct < threshold - 15.0
    val predictedStatus = when {
        isPredictedSafe -> "SAFE"
        isPredictedCritical -> "CRITICAL"
        else -> "WARNING"
    }

    val (predColor, predBg) = when {
        isPredictedSafe -> Color(0xFF155724) to Color(0xFFD4EDDA)
        isPredictedCritical -> Color(0xFF721C24) to Color(0xFFF8D7DA)
        else -> Color(0xFF856404) to Color(0xFFFFF3CD)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Simulate Future Classes for ${summary.subjectName}:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Attend counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Attend future: +$futureAttended",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (futureAttended > 0) onFutureAttendedChange(futureAttended - 1) },
                    enabled = futureAttended > 0
                ) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "$futureAttended",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { onFutureAttendedChange(futureAttended + 1) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        // Bunk counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bunk future: +$futureBunked",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (futureBunked > 0) onFutureBunkedChange(futureBunked - 1) },
                    enabled = futureBunked > 0
                ) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "$futureBunked",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = { onFutureBunkedChange(futureBunked + 1) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Predicted Result Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Predicted Attendance:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", predictedPct),
                    style = MaterialTheme.typography.titleLarge,
                    color = predColor
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = predBg
            ) {
                Text(
                    text = predictedStatus,
                    color = predColor,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
