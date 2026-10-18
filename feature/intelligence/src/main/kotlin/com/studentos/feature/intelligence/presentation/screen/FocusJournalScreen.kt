package com.studentos.feature.intelligence.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studentos.core.database.entity.ActivityLogEntity
import com.studentos.core.database.entity.CommitmentEntity
import com.studentos.core.database.entity.GoalEntity
import com.studentos.core.database.entity.JournalEntryEntity
import com.studentos.core.ui.component.ActionIconButton
import com.studentos.core.ui.component.PriorityBadge
import com.studentos.core.ui.component.SectionHeader
import com.studentos.core.ui.component.StatusPill
import com.studentos.core.ui.component.StudentOsEmptyState
import com.studentos.core.ui.theme.StudentOsTheme
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
        onExtractEntry = viewModel::extractExistingJournalEntry,
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
    onExtractEntry: (JournalEntryEntity) -> Unit,
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
    val colors = StudentOsTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }
    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
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
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = todayFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                },
                actions = {
                    ActionIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        onClick = onSettingsClick
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.appBackground,
                    titleContentColor = colors.textPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.appBackground,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                SectionHeader(
                    title = "Today's Accomplishments",
                    count = uiState.todayActivities.size,
                    icon = Icons.Default.CheckCircle
                )
            }

            if (uiState.todayActivities.isEmpty()) {
                item {
                    StudentOsEmptyState(
                        icon = Icons.Default.CheckCircle,
                        title = "No Accomplishments Logged",
                        description = "Extract your daily notes or confirm activities above to log accomplishments."
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
                SectionHeader(
                    title = "Pending Commitments",
                    count = uiState.pendingCommitments.size,
                    icon = Icons.AutoMirrored.Filled.List
                )
            }

            if (uiState.pendingCommitments.isEmpty()) {
                item {
                    StudentOsEmptyState(
                        icon = Icons.Default.Star,
                        title = "All Caught Up",
                        description = "No outstanding commitments or deadlines pending."
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
                SectionHeader(
                    title = "Journal History",
                    count = uiState.recentJournalEntries.size,
                    icon = Icons.Default.DateRange
                )
            }

            if (uiState.recentJournalEntries.isEmpty()) {
                item {
                    StudentOsEmptyState(
                        icon = Icons.Default.Edit,
                        title = "No Journal Notes Recorded",
                        description = "Notes and daily summaries will be archived here."
                    )
                }
            } else {
                items(uiState.recentJournalEntries.take(10), key = { it.id }) { entry ->
                    JournalHistoryItem(
                        entry = entry,
                        isExtracting = uiState.isExtracting,
                        onExtractClick = { onExtractEntry(entry) }
                    )
                }
            }
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
    val colors = StudentOsTheme.colors
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurface),
        border = BorderStroke(1.dp, colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.badgeVioletBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = colors.brightAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "Daily Journal & Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                if (isOfflineMode) {
                    Surface(
                        color = colors.destructiveBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, colors.destructiveBorder)
                    ) {
                        Text(
                            text = "Offline Mode",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.destructiveText,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text Input Field — compact ~3 lines
            OutlinedTextField(
                value = rawInput,
                onValueChange = onInputChanged,
                placeholder = {
                    Text(
                        "Log your tasks, study sessions, or commitments...\n(e.g., 'Solved 4 Array problems in 45m. Submit ML assignment by Friday.')",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted
                    )
                },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExtracting,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primaryCta,
                    unfocusedBorderColor = colors.innerBorder,
                    focusedContainerColor = colors.innerSurface,
                    unfocusedContainerColor = colors.innerSurface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✦ AI auto-tracks goals",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.brightAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Secondary action: Save note without extraction
                    OutlinedButton(
                        onClick = onSaveNoteOnly,
                        enabled = rawInput.isNotBlank() && !isExtracting,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, colors.innerBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textSecondary,
                            disabledContentColor = colors.textMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Save Note", style = MaterialTheme.typography.labelMedium)
                    }

                    // Primary action: Extract with AI & log accomplishments
                    Button(
                        onClick = onSubmitExtraction,
                        enabled = rawInput.isNotBlank() && !isExtracting,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryCta,
                            contentColor = Color.White,
                            disabledContainerColor = colors.innerSurface,
                            disabledContentColor = colors.textMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isExtracting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Extracting...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Extract & Log", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
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
    val colors = StudentOsTheme.colors
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurface),
        border = BorderStroke(1.5.dp, colors.primaryCta),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.badgeVioletBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.brightAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Verify Extracted Records",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                TextButton(
                    onClick = onDiscard,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Discard",
                        tint = colors.destructiveText,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discard", color = colors.destructiveText, style = MaterialTheme.typography.labelMedium)
                }
            }

            if (preview.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = preview.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }

            if (preview.clarifications.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = colors.innerSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.innerBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = colors.brightAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clarifications & Notes",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.brightAccent
                            )
                        }
                        preview.clarifications.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Extracted Activities ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activities (${preview.activities.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.brightAccent
                )
                TextButton(onClick = onAddActivity) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Activity", style = MaterialTheme.typography.labelSmall, color = colors.brightAccent)
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

            Spacer(modifier = Modifier.height(6.dp))

            // ── Extracted Commitments ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Commitments & Deadlines (${preview.commitments.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                TextButton(onClick = onAddCommitment) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Commitment", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
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

            // Confirmation Action — Solid Violet Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryCta,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Record", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
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
    val colors = StudentOsTheme.colors
    Surface(
        color = colors.innerSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, colors.innerBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = activity.title,
                    onValueChange = { title -> onUpdate { it.copy(title = title) } },
                    label = { Text("Activity Description", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryCta,
                        unfocusedBorderColor = colors.innerBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = colors.destructiveText,
                        modifier = Modifier.size(16.dp)
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
                    label = { Text("Type", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1.1f),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryCta,
                        unfocusedBorderColor = colors.innerBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = activity.metricDelta?.toString() ?: "",
                    onValueChange = { deltaStr ->
                        val delta = deltaStr.toDoubleOrNull()
                        onUpdate { it.copy(metricDelta = delta) }
                    },
                    label = { Text("Count", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(0.9f),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryCta,
                        unfocusedBorderColor = colors.innerBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = activity.durationMinutes?.toString() ?: "",
                    onValueChange = { durStr ->
                        val dur = durStr.toIntOrNull()
                        onUpdate { it.copy(durationMinutes = dur) }
                    },
                    label = { Text("Dur (m)", style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("—") },
                    modifier = Modifier.weight(0.9f),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryCta,
                        unfocusedBorderColor = colors.innerBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
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
    val colors = StudentOsTheme.colors
    var expanded by remember { mutableStateOf(false) }
    val currentGoal = goals.find { it.id == selectedGoalId }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Linked Goal: ",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        Box {
            Surface(
                color = if (currentGoal != null) colors.badgeVioletBg else colors.innerSurface,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (currentGoal != null) colors.primaryCta else colors.innerBorder),
                modifier = Modifier.clickable { expanded = true }
            ) {
                Text(
                    text = currentGoal?.title ?: "None (Unlinked)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (currentGoal != null) colors.brightAccent else colors.textMuted,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(colors.cardSurface)
            ) {
                DropdownMenuItem(
                    text = { Text("None (Unlinked)", color = colors.textSecondary) },
                    onClick = {
                        onGoalSelected(null)
                        expanded = false
                    }
                )
                goals.forEach { goal ->
                    DropdownMenuItem(
                        text = { Text("${goal.title} (${goal.category})", color = colors.textPrimary) },
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
    val colors = StudentOsTheme.colors
    Surface(
        color = colors.innerSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, colors.innerBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = commitment.title,
                    onValueChange = { title -> onUpdate { it.copy(title = title) } },
                    label = { Text("Commitment Title", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryCta,
                        unfocusedBorderColor = colors.innerBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = colors.destructiveText,
                        modifier = Modifier.size(16.dp)
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
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryCta,
                        unfocusedBorderColor = colors.innerBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                OutlinedTextField(
                    value = commitment.priority,
                    onValueChange = { prio ->
                        onUpdate { it.copy(priority = prio.uppercase()) }
                    },
                    label = { Text("Priority", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(0.9f),
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryCta,
                        unfocusedBorderColor = colors.innerBorder,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
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
    val colors = StudentOsTheme.colors
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurface),
        border = BorderStroke(1.dp, colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = colors.badgeVioletBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, colors.primaryCta.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = activity.activityType,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.brightAccent,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                val timeFormatted = remember(activity.timestamp) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(activity.timestamp))
                }
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = activity.summary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activity.metricDelta > 0) {
                    Surface(
                        color = colors.successBg,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, colors.successText.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "+${if (activity.metricDelta % 1.0 == 0.0) activity.metricDelta.toInt().toString() else activity.metricDelta.toString()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.successText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                val durationMinutes = activity.durationMinutes
                val durText = if (durationMinutes != null) "$durationMinutes min" else "Duration unrecorded"
                Text(
                    text = durText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                if (!goalTitle.isNullOrBlank()) {
                    Text(
                        text = "• $goalTitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.brightAccent,
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
    val colors = StudentOsTheme.colors
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurface),
        border = BorderStroke(1.dp, colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionIconButton(
                icon = Icons.Default.CheckCircle,
                contentDescription = "Mark Complete",
                onClick = onComplete,
                tint = colors.primaryCta
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commitment.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PriorityBadge(priority = commitment.priority)

                    val deadlineMs = commitment.deadline
                    if (deadlineMs != null) {
                        val deadlineStr = remember(deadlineMs) {
                            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(deadlineMs))
                        }
                        Text(
                            text = "Due: $deadlineStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    } else {
                        Text(
                            text = "No deadline",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
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
    isExtracting: Boolean,
    onExtractClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = StudentOsTheme.colors
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurface),
        border = BorderStroke(1.dp, colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = colors.innerSurface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, colors.innerBorder)
                ) {
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                if (entry.structuredSummary != null) {
                    StatusPill(
                        label = "Extracted",
                        icon = Icons.Default.Check,
                        containerColor = colors.successBg,
                        contentColor = colors.successText,
                        borderColor = colors.successText.copy(alpha = 0.3f)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusPill(
                            label = "Raw Note",
                            containerColor = colors.innerSurface,
                            contentColor = colors.textMuted,
                            borderColor = colors.innerBorder
                        )

                        // Discoverable extraction action for unextracted notes
                        Button(
                            onClick = onExtractClick,
                            enabled = !isExtracting,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primaryCta,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Extract with AI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.rawLog,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 3
            )

            if (!entry.structuredSummary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = colors.innerSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.innerBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Summary: ${entry.structuredSummary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
