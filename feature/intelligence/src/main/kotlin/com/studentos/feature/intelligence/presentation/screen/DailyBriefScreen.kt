package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studentos.core.intelligence.snapshot.AssignmentUrgentSnapshot
import com.studentos.core.intelligence.snapshot.AttendanceWarningSnapshot
import com.studentos.core.intelligence.snapshot.FreeSlotSnapshot
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.presentation.component.RecommendationCardItem
import com.studentos.feature.intelligence.presentation.component.ScoreSummaryCard
import com.studentos.feature.intelligence.presentation.state.DailyBriefUiState

/**
 * DailyBriefScreen — Redesigned and polished Daily Brief screen matching Student OS Home UI/UX.
 *
 * Major Sections:
 * 1. Header (Back arrow + Title + History action)
 * 2. Today's Progress Hero Card (Dominant purple card, accurate zero-state, single engine badge)
 * 3. AI / Offline Guidance Card (Clean, concise, readable guidance without duplicate badges)
 * 4. Today's Recommendations (Clean actionable cards, non-repetitive empty state)
 * 5. Contextual Highlights (Attendance warnings, urgent assignments, free slot suggestions)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyBriefScreen(
    uiState: DailyBriefUiState,
    onGenerateClick: () -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Daily Brief",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Text(
                        text = "History →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(onClick = onHistoryClick)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading || uiState.isGenerating -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.isGenerating) "Generating Today's Brief…" else "Loading Brief…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onRetryClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Retry")
                        }
                    }
                }

                uiState.isEmpty || uiState.dailyBrief == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No Daily Brief available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generate your daily brief to get actionable recommendations and priority scores for today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onGenerateClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Generate Today's Brief",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                else -> {
                    val brief = uiState.dailyBrief
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // ── 1. Today's Progress Hero Card ──────────────────────────
                        ScoreSummaryCard(
                            date = brief.date,
                            scoreTarget = brief.scoreTarget,
                            scoreActual = brief.scoreActual,
                            guidanceSource = brief.guidanceSource
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── 2. AI Guidance Card ─────────────────────────────────────
                        GuidanceCard(brief = brief)

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── 3. Today's Recommendations ──────────────────────────────
                        RecommendationsSection(
                            recommendations = uiState.recommendations,
                            onNavigate = onNavigate
                        )

                        // ── 4. Attendance Warnings (if any) ─────────────────────────
                        if (uiState.attendanceWarnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            AttendanceWarningsSection(
                                warnings = uiState.attendanceWarnings,
                                onNavigate = onNavigate
                            )
                        }

                        // ── 5. Urgent Assignments (if any) ──────────────────────────
                        if (uiState.urgentAssignments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            UrgentAssignmentsSection(
                                assignments = uiState.urgentAssignments,
                                onNavigate = onNavigate
                            )
                        }

                        // ── 6. Free Slot Suggestions (if any) ───────────────────────
                        if (uiState.freeSlots.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            FreeSlotsSection(slots = uiState.freeSlots)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────────
// Sub-components
// ────────────────────────────────────────────────────────────────────────────────

@Composable
private fun GuidanceCard(brief: DailyBrief) {
    val isLLM = brief.guidanceSource == DailyBrief.GUIDANCE_SOURCE_LLM
    val title = if (isLLM) "AI Guidance" else "Offline Guidance"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            val guidanceContent = (brief.llmGuidance ?: brief.briefJson).trim()
            val displayGuidance = if (guidanceContent.isNotBlank()) {
                guidanceContent
            } else {
                if (isLLM) "No AI guidance available for today." else "No offline guidance available for today."
            }

            Text(
                text = displayGuidance,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecommendationsSection(
    recommendations: List<com.studentos.feature.intelligence.domain.model.RecommendationCard>,
    onNavigate: (String) -> Unit
) {
    Text(
        text = "Today's Recommendations",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (recommendations.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "No recommendations for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            recommendations.forEach { card ->
                RecommendationCardItem(
                    card = card,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
private fun AttendanceWarningsSection(
    warnings: List<AttendanceWarningSnapshot>,
    onNavigate: (String) -> Unit
) {
    Text(
        text = "Attendance Warnings",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        warnings.forEach { warning ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("weekly") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = warning.subject,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Current: ${warning.percentage}% • Target: ${warning.threshold}% (Must attend ${warning.mustAttend} classes)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UrgentAssignmentsSection(
    assignments: List<AssignmentUrgentSnapshot>,
    onNavigate: (String) -> Unit
) {
    Text(
        text = "Urgent Assignments",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        assignments.forEach { assignment ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("assignments/list") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Assignment",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = assignment.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${assignment.subject} • Due in ${assignment.hoursRemaining}h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FreeSlotsSection(slots: List<FreeSlotSnapshot>) {
    Text(
        text = "Free Slot Suggestions",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slots.forEach { slot ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Slot",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${slot.start} – ${slot.end} (${slot.durationMinutes} mins)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Recommended: Revise DSA or work on pending project tasks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
