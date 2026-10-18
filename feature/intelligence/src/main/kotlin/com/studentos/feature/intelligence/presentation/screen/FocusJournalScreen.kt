package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentos.core.database.entity.ActivityLogEntity
import com.studentos.core.database.entity.CommitmentEntity
import com.studentos.core.database.entity.GoalEntity
import com.studentos.core.database.entity.JournalEntryEntity
import com.studentos.feature.intelligence.domain.model.EditableActivity
import com.studentos.feature.intelligence.domain.model.EditableCommitment
import com.studentos.feature.intelligence.presentation.state.ExtractionPreview
import com.studentos.feature.intelligence.presentation.state.FocusJournalUiState
import com.studentos.feature.intelligence.presentation.viewmodel.FocusJournalViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun FocusJournalRoute(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusJournalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    FocusJournalScreen(
        uiState = uiState,
        onSettingsClick = onSettingsClick,
        onInputChanged = viewModel::onRawInputChanged,
        onSubmitExtraction = viewModel::submitForExtraction,
        onSaveNoteOnly = viewModel::saveRawNoteOnly,
        onUpdateActivity = viewModel::updateActivity,
        onRemoveActivity = viewModel::removeActivity,
        onAddActivity = viewModel::addEmptyActivity,
        onUpdateCommitment = viewModel::updateCommitment,
        onRemoveCommitment = viewModel::removeCommitment,
        onAddCommitment = viewModel::addEmptyCommitment,
        onConfirmPreview = viewModel::confirmPreview,
        onDiscardPreview = viewModel::discardPreview,
        onToggleCommitment = viewModel::toggleCommitmentComplete,
        onClearMessages = viewModel::clearMessages,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusJournalScreen(
    uiState: FocusJournalUiState,
    onSettingsClick: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSubmitExtraction: () -> Unit,
    onSaveNoteOnly: () -> Unit,
    onUpdateActivity: (String, (EditableActivity) -> EditableActivity) -> Unit,
    onRemoveActivity: (String) -> Unit,
    onAddActivity: () -> Unit,
    onUpdateCommitment: (String, (EditableCommitment) -> EditableCommitment) -> Unit,
    onRemoveCommitment: (String) -> Unit,
    onAddCommitment: () -> Unit,
    onConfirmPreview: () -> Unit,
    onDiscardPreview: () -> Unit,
    onToggleCommitment: (Long) -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            onClearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Focus & Journal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = todayFormatted,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // ── Section 1: Daily Journal Input ─────────────────────────────────
            item {
                JournalInputCard(
                    rawInput = uiState.rawInput,
                    isExtracting = uiState.isExtracting,
                    isOfflineMode = uiState.isOfflineMode,
                    onInputChanged = onInputChanged,
                    onSubmitExtraction = onSubmitExtraction,
                    onSaveNoteOnly = onSaveNoteOnly
                )
            }

            // ── Section 2: AI Extraction Confirmation Preview ──────────────────
            if (uiState.preview != null) {
                item {
                    ExtractionPreviewCard(
                        preview = uiState.preview,
                        activeGoals = uiState.activeGoals,
                        onUpdateActivity = onUpdateActivity,
                        onRemoveActivity = onRemoveActivity,
                        onAddActivity = onAddActivity,
                        onUpdateCommitment = onUpdateCommitment,
                        onRemoveCommitment = onRemoveCommitment,
                        onAddCommitment = onAddCommitment,
                        onConfirm = onConfirmPreview,
                        onDiscard = onDiscardPreview
                    )
                }
            }

            // ── Section 3: Today's Confirmed Activities ────────────────────────
            item {
                Text(
                    text = "Today's Accomplishments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (uiState.todayActivities.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "No activities confirmed for today yet. Write in your journal above to log your accomplishments!"
                    )
                }
            } else {
                items(uiState.todayActivities, key = { it.id }) { activity ->
                    ActivityLogItem(
                        activity = activity,
                        goalTitle = uiState.activeGoals.find { it.id == activity.goalId }?.title
                    )
                }
            }

            // ── Section 4: Pending Commitments & Deadlines ──────────────────────
            item {
                Text(
                    text = "Pending Commitments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (uiState.pendingCommitments.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "No pending commitments right now. All caught up!"
                    )
                }
            } else {
                items(uiState.pendingCommitments, key = { it.id }) { commitment ->
                    CommitmentItem(
                        commitment = commitment,
                        onComplete = { onToggleCommitment(commitment.id) }
                    )
                }
            }

            // ── Section 5: Journal History ─────────────────────────────────────
            item {
                Text(
                    text = "Journal History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (uiState.recentJournalEntries.isEmpty()) {
                item {
                    EmptyStateCard(
                        message = "No journal history recorded yet."
                    )
                }
            } else {
                items(uiState.recentJournalEntries.take(10), key = { it.id }) { entry ->
                    JournalHistoryItem(entry = entry)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// ── Components ─────────────────────────────────────────────────────────────────

@Composable
fun JournalInputCard(
    rawInput: String,
    isExtracting: Boolean,
    isOfflineMode: Boolean,
    onInputChanged: (String) -> Unit,
    onSubmitExtraction: () -> Unit,
    onSaveNoteOnly: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Journal & Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (isOfflineMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Offline Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = rawInput,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(
                        "Log what you accomplished, studied, or plan to do... (e.g. 'Solved 3 LeetCode problems in 45m. Submit OS lab by Friday 5pm.')",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExtracting,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSaveNoteOnly,
                    enabled = rawInput.isNotBlank() && !isExtracting,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Note")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSubmitExtraction,
                    enabled = rawInput.isNotBlank() && !isExtracting,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isExtracting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extracting...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Extract with AI")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtractionPreviewCard(
    preview: ExtractionPreview,
    activeGoals: List<GoalEntity>,
    onUpdateActivity: (String, (EditableActivity) -> EditableActivity) -> Unit,
    onRemoveActivity: (String) -> Unit,
    onAddActivity: () -> Unit,
    onUpdateCommitment: (String, (EditableCommitment) -> EditableCommitment) -> Unit,
    onRemoveCommitment: (String) -> Unit,
    onAddCommitment: () -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verify Extracted Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                TextButton(onClick = onDiscard) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Discard",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard")
                }
            }

            if (preview.summary.isNotBlank()) {
                Text(
                    text = preview.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (preview.clarifications.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clarifications & Notes:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        preview.clarifications.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Extracted Activities ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activities (${preview.activities.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onAddActivity) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Activity", style = MaterialTheme.typography.labelSmall)
                }
            }

            preview.activities.forEach { act ->
                EditableActivityItem(
                    activity = act,
                    activeGoals = activeGoals,
                    onUpdate = { update -> onUpdateActivity(act.tempId, update) },
                    onDelete = { onRemoveActivity(act.tempId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Extracted Commitments ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Commitments & Deadlines (${preview.commitments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onAddCommitment) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Commitment", style = MaterialTheme.typography.labelSmall)
                }
            }

            preview.commitments.forEach { com ->
                EditableCommitmentItem(
                    commitment = com,
                    onUpdate = { update -> onUpdateCommitment(com.tempId, update) },
                    onDelete = { onRemoveCommitment(com.tempId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confirmation Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Record", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditableActivityItem(
    activity: EditableActivity,
    activeGoals: List<GoalEntity>,
    onUpdate: ((EditableActivity) -> EditableActivity) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = activity.title,
                    onValueChange = { title -> onUpdate { it.copy(title = title) } },
                    label = { Text("Activity Description", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = activity.activityType,
                    onValueChange = { type -> onUpdate { it.copy(activityType = type.uppercase()) } },
                    label = { Text("Type (e.g. DSA, PROJECT)", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = activity.metricDelta?.toString() ?: "",
                    onValueChange = { deltaStr ->
                        val delta = deltaStr.toDoubleOrNull()
                        onUpdate { it.copy(metricDelta = delta) }
                    },
                    label = { Text("Count", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = activity.durationMinutes?.toString() ?: "",
                    onValueChange = { durStr ->
                        val dur = durStr.toIntOrNull()
                        onUpdate { it.copy(durationMinutes = dur) }
                    },
                    label = { Text("Duration (m)", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("Unknown") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            if (activeGoals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                GoalSelectorRow(
                    selectedGoalId = activity.matchedGoalId,
                    goals = activeGoals,
                    onGoalSelected = { goalId -> onUpdate { it.copy(matchedGoalId = goalId) } }
                )
            }
        }
    }
}

@Composable
fun GoalSelectorRow(
    selectedGoalId: Long?,
    goals: List<GoalEntity>,
    onGoalSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currentGoal = goals.find { it.id == selectedGoalId }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Linked Goal: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box {
            Surface(
                color = if (currentGoal != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(
                    text = currentGoal?.title ?: "None (Unlinked)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (currentGoal != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("None (Unlinked)") },
                    onClick = {
                        onGoalSelected(null)
                        expanded = false
                    }
                )
                goals.forEach { goal ->
                    DropdownMenuItem(
                        text = { Text("${goal.title} (${goal.category})") },
                        onClick = {
                            onGoalSelected(goal.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EditableCommitmentItem(
    commitment: EditableCommitment,
    onUpdate: ((EditableCommitment) -> EditableCommitment) -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commitment.title,
                    onValueChange = { title -> onUpdate { it.copy(title = title) } },
                    label = { Text("Commitment Title", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commitment.targetDate ?: "",
                    onValueChange = { dateStr ->
                        onUpdate { it.copy(targetDate = dateStr.ifBlank { null }) }
                    },
                    label = { Text("Date (YYYY-MM-DD)", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("No deadline") },
                    modifier = Modifier.weight(1.2f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = commitment.priority,
                    onValueChange = { prio ->
                        onUpdate { it.copy(priority = prio.uppercase()) }
                    },
                    label = { Text("Priority", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(0.9f),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun ActivityLogItem(
    activity: ActivityLogEntity,
    goalTitle: String?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = activity.activityType,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                val timeFormatted = remember(activity.timestamp) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(activity.timestamp))
                }
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = activity.summary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activity.metricDelta > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "+${if (activity.metricDelta % 1.0 == 0.0) activity.metricDelta.toInt().toString() else activity.metricDelta.toString()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val durationMinutes = activity.durationMinutes
                val durText = if (durationMinutes != null) "$durationMinutes min" else "Duration unrecorded"
                Text(
                    text = durText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!goalTitle.isNullOrBlank()) {
                    Text(
                        text = "• $goalTitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CommitmentItem(
    commitment: CommitmentEntity,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onComplete) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Mark Complete",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commitment.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val prioColor = when (commitment.priority) {
                        "HIGH" -> MaterialTheme.colorScheme.error
                        "LOW" -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        color = prioColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = commitment.priority,
                            style = MaterialTheme.typography.labelSmall,
                            color = prioColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    val deadlineMs = commitment.deadline
                    if (deadlineMs != null) {
                        val deadlineStr = remember(deadlineMs) {
                            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(deadlineMs))
                        }
                        Text(
                            text = "Due: $deadlineStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No deadline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JournalHistoryItem(
    entry: JournalEntryEntity,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.date,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (entry.structuredSummary != null) {
                    Text(
                        text = "Structured",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.rawLog,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )

            if (!entry.structuredSummary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Summary: ${entry.structuredSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
