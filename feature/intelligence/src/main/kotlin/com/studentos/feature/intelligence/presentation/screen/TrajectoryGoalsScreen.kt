package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentos.core.database.entity.GoalEntity
import com.studentos.feature.intelligence.domain.model.GoalProgress
import com.studentos.feature.intelligence.domain.model.GoalStatus
import com.studentos.feature.intelligence.presentation.state.GoalDialogState
import com.studentos.feature.intelligence.presentation.state.GoalsTab
import com.studentos.feature.intelligence.presentation.state.TrajectoryGoalsUiState
import com.studentos.feature.intelligence.presentation.viewmodel.TrajectoryGoalsViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun TrajectoryGoalsRoute(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrajectoryGoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    TrajectoryGoalsScreen(
        uiState = uiState,
        onSettingsClick = onSettingsClick,
        onSelectTab = viewModel::selectTab,
        onToggleGraph = viewModel::toggleGraphExpanded,
        onOpenCreate = viewModel::openCreateGoalDialog,
        onOpenEdit = viewModel::openEditGoalDialog,
        onDismissDialog = viewModel::dismissDialog,
        onSaveGoal = viewModel::saveGoal,
        onToggleArchive = viewModel::toggleArchive,
        onPromptDelete = viewModel::promptDelete,
        onDismissDeletePrompt = viewModel::dismissDeletePrompt,
        onConfirmDelete = viewModel::confirmDelete,
        onClearMessages = viewModel::clearMessages,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrajectoryGoalsScreen(
    uiState: TrajectoryGoalsUiState,
    onSettingsClick: () -> Unit,
    onSelectTab: (GoalsTab) -> Unit,
    onToggleGraph: (Long) -> Unit,
    onOpenCreate: () -> Unit,
    onOpenEdit: (GoalEntity) -> Unit,
    onDismissDialog: () -> Unit,
    onSaveGoal: (Long, String, String, String, String, Double, Double, Long, Long, Boolean) -> Unit,
    onToggleArchive: (GoalEntity) -> Unit,
    onPromptDelete: (GoalEntity) -> Unit,
    onDismissDeletePrompt: () -> Unit,
    onConfirmDelete: () -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Trajectory & Goals",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Long-term measurable execution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Goal")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs: Active vs Archived
            TabRow(
                selectedTabIndex = if (uiState.selectedTab == GoalsTab.ACTIVE) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedTab == GoalsTab.ACTIVE,
                    onClick = { onSelectTab(GoalsTab.ACTIVE) },
                    text = {
                        Text("Active (${uiState.activeGoals.size})", fontWeight = FontWeight.SemiBold)
                    }
                )
                Tab(
                    selected = uiState.selectedTab == GoalsTab.ARCHIVED,
                    onClick = { onSelectTab(GoalsTab.ARCHIVED) },
                    text = {
                        Text("Archived (${uiState.archivedGoals.size})", fontWeight = FontWeight.SemiBold)
                    }
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val currentList = if (uiState.selectedTab == GoalsTab.ACTIVE) {
                    uiState.activeGoals
                } else {
                    uiState.archivedGoals
                }

                if (currentList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.selectedTab == GoalsTab.ACTIVE) {
                                "No active goals tracked yet.\nTap '+' to create your first goal!"
                            } else {
                                "No archived goals."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(currentList, key = { it.goal.id }) { goalProgress ->
                            val isGraphExpanded = uiState.expandedGraphGoalIds.contains(goalProgress.goal.id)

                            GoalCard(
                                progress = goalProgress,
                                isGraphExpanded = isGraphExpanded,
                                onToggleGraph = { onToggleGraph(goalProgress.goal.id) },
                                onEdit = { onOpenEdit(goalProgress.goal) },
                                onToggleArchive = { onToggleArchive(goalProgress.goal) },
                                onDelete = { onPromptDelete(goalProgress.goal) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            }
        }
    }

    // ── Dialog: Create / Edit Goal ─────────────────────────────────────────────
    if (uiState.dialogState != null) {
        GoalFormDialog(
            state = uiState.dialogState,
            onDismiss = onDismissDialog,
            onSave = onSaveGoal
        )
    }

    // ── Dialog: Confirm Deletion ───────────────────────────────────────────────
    if (uiState.deleteConfirmGoal != null) {
        val goalToDelete = uiState.deleteConfirmGoal
        AlertDialog(
            onDismissRequest = onDismissDeletePrompt,
            title = { Text("Delete Goal") },
            text = {
                Text(
                    "Are you sure you want to delete '${goalToDelete.title}'?\n\n" +
                            "All historical activity logs linked to this goal will be safely preserved with unlinked goal status."
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissDeletePrompt) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Goal Card Component ────────────────────────────────────────────────────────

@Composable
fun GoalCard(
    progress: GoalProgress,
    isGraphExpanded: Boolean,
    onToggleGraph: () -> Unit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goal = progress.goal
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category chip, Metric Type chip, and Pace Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = goal.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = goal.metricType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                GoalStatusBadge(status = progress.status, description = progress.statusDescription)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics & Progress Fraction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val actualFormatted = if (progress.actualProgress % 1.0 == 0.0) {
                    progress.actualProgress.toInt().toString()
                } else {
                    "%.1f".format(progress.actualProgress)
                }
                val targetFormatted = if (goal.targetValue % 1.0 == 0.0) {
                    goal.targetValue.toInt().toString()
                } else {
                    "%.1f".format(goal.targetValue)
                }

                Column {
                    Text(
                        text = "$actualFormatted / $targetFormatted ${goal.unit}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Baseline: ${goal.startValue} ${goal.unit} • ${progress.totalLogsCount} logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "%.1f%%".format(progress.progressPercentage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (progress.progressPercentage >= 100.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar (bounded 0f..1f for UI drawing, but underlying metric is unclamped)
            val visualProgress = (progress.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { visualProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when (progress.status) {
                    GoalStatus.COMPLETED -> Color(0xFF10B981)
                    GoalStatus.AHEAD -> Color(0xFF059669)
                    GoalStatus.BEHIND -> Color(0xFFF59E0B)
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Target Date & Days Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Target: ${dateFormat.format(Date(goal.targetDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val remainingText = if (progress.daysRemaining != null && progress.daysRemaining > 0) {
                    "${progress.daysRemaining} days left"
                } else if (System.currentTimeMillis() >= goal.targetDate) {
                    "Deadline reached"
                } else {
                    "Starts soon"
                }
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: View Progress Graph (Toggle), Edit, Archive, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onToggleGraph,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isGraphExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isGraphExpanded) "Hide Graph" else "View Progress Graph", style = MaterialTheme.typography.labelMedium)
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Goal", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleArchive) {
                        Icon(
                            imageVector = if (goal.isArchived) Icons.Default.Refresh else Icons.Default.CheckCircle,
                            contentDescription = if (goal.isArchived) "Unarchive Goal" else "Archive Goal",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Goal",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Inline Expandable Trajectory Graph
            AnimatedVisibility(
                visible = isGraphExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    InlineProgressGraph(progress = progress)
                }
            }
        }
    }
}

// ── Inline Trajectory & Goals Canvas Graph ─────────────────────────────────────

@Composable
fun InlineProgressGraph(
    progress: GoalProgress,
    modifier: Modifier = Modifier
) {
    val goal = progress.goal
    val points = progress.historyPoints

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Graph Header & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expected vs Actual Trajectory",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp, 2.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Actual", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp, 2.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Expected", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Check edge cases where graph cannot be plotted
            if (goal.targetDate <= goal.startDate) {
                Text(
                    text = "Cannot compute trajectory: Target date must be after start date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                return@Column
            }

            // Canvas Line Chart
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outline
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                val paddingX = 16f
                val paddingY = 16f
                val chartWidth = size.width - (paddingX * 2)
                val chartHeight = size.height - (paddingY * 2)

                val minX = goal.startDate
                val maxX = max(goal.targetDate, max(System.currentTimeMillis(), points.maxOfOrNull { it.timestamp } ?: goal.targetDate))
                val timeSpan = max(1L, maxX - minX).toDouble()

                val minY = min(0.0, min(goal.startValue, points.minOfOrNull { it.actualProgress } ?: goal.startValue))
                val maxY = max(goal.targetValue, max(progress.actualProgress, points.maxOfOrNull { it.actualProgress } ?: goal.targetValue)) * 1.05
                val valueSpan = max(1.0, maxY - minY)

                fun toPxX(t: Long): Float {
                    val ratio = ((t - minX).toDouble() / timeSpan).coerceIn(0.0, 1.0)
                    return paddingX + (ratio.toFloat() * chartWidth)
                }

                fun toPxY(v: Double): Float {
                    val ratio = ((v - minY) / valueSpan).coerceIn(0.0, 1.0)
                    return paddingY + ((1.0 - ratio).toFloat() * chartHeight)
                }

                // 1. Draw Grid Lines (Baseline and Target)
                val yTarget = toPxY(goal.targetValue)
                val yBaseline = toPxY(goal.startValue)

                drawLine(
                    color = gridColor,
                    start = Offset(paddingX, yBaseline),
                    end = Offset(size.width - paddingX, yBaseline),
                    strokeWidth = 1f
                )

                drawLine(
                    color = gridColor,
                    start = Offset(paddingX, yTarget),
                    end = Offset(size.width - paddingX, yTarget),
                    strokeWidth = 1f
                )

                // 2. Draw Expected Trajectory Line (Dashed)
                val expectedStart = Offset(toPxX(goal.startDate), toPxY(goal.startValue))
                val expectedEnd = Offset(toPxX(goal.targetDate), toPxY(goal.targetValue))

                drawLine(
                    color = outlineColor,
                    start = expectedStart,
                    end = expectedEnd,
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // 3. Draw Actual Progress Curve
                if (points.isNotEmpty()) {
                    val actualPath = Path()
                    val fillPath = Path()

                    val firstPoint = points.first()
                    val startX = toPxX(firstPoint.timestamp)
                    val startY = toPxY(firstPoint.actualProgress)

                    actualPath.moveTo(startX, startY)
                    fillPath.moveTo(startX, toPxY(minY))
                    fillPath.lineTo(startX, startY)

                    points.forEach { point ->
                        val px = toPxX(point.timestamp)
                        val py = toPxY(point.actualProgress)
                        actualPath.lineTo(px, py)
                        fillPath.lineTo(px, py)

                        // Draw point marker
                        drawCircle(
                            color = primaryColor,
                            radius = 4.5f,
                            center = Offset(px, py)
                        )
                    }

                    val lastX = toPxX(points.last().timestamp)
                    fillPath.lineTo(lastX, toPxY(minY))
                    fillPath.close()

                    // Fill gradient under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = toPxY(maxY),
                            endY = toPxY(minY)
                        )
                    )

                    // Stroke actual line
                    drawPath(
                        path = actualPath,
                        color = primaryColor,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }
            }

            // Graph Footnote / Dates
            val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Start: ${dateFormat.format(Date(goal.startDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Target: ${dateFormat.format(Date(goal.targetDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (progress.totalLogsCount == 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• No confirmed activities logged yet. Trajectory represents planned expected progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Goal Status Badge ──────────────────────────────────────────────────────────

@Composable
fun GoalStatusBadge(status: GoalStatus, description: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, label) = when (status) {
        GoalStatus.COMPLETED -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "COMPLETED")
        GoalStatus.AHEAD -> Triple(Color(0xFFECFDF5), Color(0xFF047857), "AHEAD")
        GoalStatus.ON_TRACK -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), "ON TRACK")
        GoalStatus.BEHIND -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "BEHIND")
        GoalStatus.NOT_STARTED -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "UPCOMING")
        GoalStatus.INSUFFICIENT_DATA -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "INSUFFICIENT DATA")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ── Goal Form Dialog (Create / Edit) ───────────────────────────────────────────

@Composable
fun GoalFormDialog(
    state: GoalDialogState,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, String, Double, Double, Long, Long, Boolean) -> Unit
) {
    val isEdit = state is GoalDialogState.Edit
    val existingGoal = (state as? GoalDialogState.Edit)?.goal

    val defaultStartDate = remember {
        existingGoal?.startDate ?: System.currentTimeMillis()
    }
    val defaultTargetDate = remember {
        existingGoal?.targetDate ?: (System.currentTimeMillis() + (90L * 24L * 60L * 60L * 1000L)) // 90 days out
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    var title by remember { mutableStateOf(existingGoal?.title ?: "") }
    var category by remember { mutableStateOf(existingGoal?.category ?: "DSA") }
    var metricType by remember { mutableStateOf(existingGoal?.metricType ?: "COUNT") }
    var unit by remember { mutableStateOf(existingGoal?.unit ?: if (metricType == "COUNT") "questions" else "hours") }
    var startValueStr by remember { mutableStateOf(existingGoal?.startValue?.toString() ?: "0.0") }
    var targetValueStr by remember { mutableStateOf(existingGoal?.targetValue?.toString() ?: "100.0") }

    val startLocalDate = remember(defaultStartDate) {
        InstantToLocalDate(defaultStartDate)
    }
    val targetLocalDate = remember(defaultTargetDate) {
        InstantToLocalDate(defaultTargetDate)
    }

    var startDateStr by remember { mutableStateOf(startLocalDate.format(dateFormatter)) }
    var targetDateStr by remember { mutableStateOf(targetLocalDate.format(dateFormatter)) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (isEdit) "Edit Goal" else "Create New Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    placeholder = { Text("e.g. Solve 200 DSA Questions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category & Metric Type row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it.uppercase() },
                        label = { Text("Category") },
                        placeholder = { Text("DSA, ML, PROJECT") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Metric Type", style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = metricType == "COUNT",
                                onClick = {
                                    metricType = "COUNT"
                                    if (unit == "hours") unit = "questions"
                                },
                                label = { Text("COUNT") }
                            )
                            FilterChip(
                                selected = metricType == "HOURS",
                                onClick = {
                                    metricType = "HOURS"
                                    if (unit == "questions") unit = "hours"
                                },
                                label = { Text("HOURS") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Unit, Start Baseline, Target Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("questions") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startValueStr,
                        onValueChange = { startValueStr = it },
                        label = { Text("Baseline") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                    OutlinedTextField(
                        value = targetValueStr,
                        onValueChange = { targetValueStr = it },
                        label = { Text("Target") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dates row (YYYY-MM-DD)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDateStr,
                        onValueChange = { startDateStr = it },
                        label = { Text("Start (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetDateStr,
                        onValueChange = { targetDateStr = it },
                        label = { Text("Target (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val startVal = startValueStr.toDoubleOrNull()
                            val targetVal = targetValueStr.toDoubleOrNull()
                            val startEpoch = parseDateToEpoch(startDateStr)
                            val targetEpoch = parseDateToEpoch(targetDateStr)

                            if (title.isBlank()) {
                                validationError = "Title cannot be blank."
                            } else if (category.isBlank()) {
                                validationError = "Category cannot be blank."
                            } else if (unit.isBlank()) {
                                validationError = "Unit cannot be blank."
                            } else if (startVal == null || startVal < 0.0) {
                                validationError = "Starting baseline must be 0 or positive."
                            } else if (targetVal == null || targetVal <= startVal) {
                                validationError = "Target value must be greater than starting baseline."
                            } else if (startEpoch == null || targetEpoch == null) {
                                validationError = "Invalid date format. Use YYYY-MM-DD."
                            } else if (targetEpoch <= startEpoch) {
                                validationError = "Target deadline must be after start date."
                            } else {
                                validationError = null
                                onSave(
                                    existingGoal?.id ?: 0L,
                                    title,
                                    category,
                                    metricType,
                                    unit,
                                    startVal,
                                    targetVal,
                                    startEpoch,
                                    targetEpoch,
                                    existingGoal?.isArchived ?: false
                                )
                            }
                        }
                    ) {
                        Text(if (isEdit) "Save Changes" else "Create Goal")
                    }
                }
            }
        }
    }
}

// ── Date Helpers ───────────────────────────────────────────────────────────────

private fun InstantToLocalDate(epochMs: Long): LocalDate {
    return java.time.Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

private fun parseDateToEpoch(dateStr: String): Long? {
    return try {
        val localDate = LocalDate.parse(dateStr.trim())
        localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    } catch (_: Exception) {
        null
    }
}
