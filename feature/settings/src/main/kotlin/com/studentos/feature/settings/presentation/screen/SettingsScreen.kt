package com.studentos.feature.settings.presentation.screen

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studentos.feature.settings.presentation.component.SettingActionRow
import com.studentos.feature.settings.presentation.component.SettingChoiceRow
import com.studentos.feature.settings.presentation.component.SettingEditDialog
import com.studentos.feature.settings.presentation.component.SettingSliderRow
import com.studentos.feature.settings.presentation.component.SettingSwitchRow
import com.studentos.feature.settings.presentation.component.SettingTimePickerDialog
import com.studentos.feature.settings.presentation.component.SettingsSection
import com.studentos.feature.settings.presentation.state.SettingsUiState
import com.studentos.feature.settings.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    onNavigateToAiDiagnostics: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onNavigateToAiDiagnostics = onNavigateToAiDiagnostics,
        onAttendanceThresholdChange = viewModel::updateAttendanceThreshold,
        onOcrConfidenceChange = viewModel::updateOcrConfidenceThreshold,
        onAiEnabledChange = viewModel::updateAiEnabled,
        onAiProviderChange = viewModel::updateAiProvider,
        onDeepSeekApiKeyChange = viewModel::updateDeepSeekApiKey,
        onAiIntradayUpdatesChange = viewModel::updateAiIntradayUpdates,
        onAiTonePreferenceChange = viewModel::updateAiTonePreference,
        onAiMaxCallsChange = viewModel::updateAiMaxCallsPerDay,
        onAiCacheMaxAgeChange = viewModel::updateAiCacheMaxAgeHours,
        onCodeChefHandleChange = viewModel::updateCodeChefHandle,
        onCodeforcesHandleChange = viewModel::updateCodeforcesHandle,
        onCpSyncIntervalChange = viewModel::updateCpSyncInterval,
        onShowTimePicker = viewModel::showTimePickerDialog,
        onDismissTimePicker = viewModel::dismissTimePickerDialog,
        onDailyBriefTimeChange = viewModel::updateDailyBriefTime,
        onScoreWeightClassChange = viewModel::updateScoreWeightClass,
        onScoreWeightAssignmentChange = viewModel::updateScoreWeightAssignment,
        onScoreWeightProjectChange = viewModel::updateScoreWeightProjectAction,
        onScoreWeightDsaChange = viewModel::updateScoreWeightDsa,
        onNotificationDailyBriefChange = viewModel::updateNotificationDailyBrief,
        onNotificationAssignmentReminderChange = viewModel::updateNotificationAssignmentReminder,
        onNotificationClassReminderChange = viewModel::updateNotificationClassReminder,
        onNotificationClassReminderLeadChange = viewModel::updateNotificationClassReminderLead,
        onNotificationContestReminderChange = viewModel::updateNotificationContestReminder,
        onNotificationFreeSlotChange = viewModel::updateNotificationFreeSlot,
        onNotificationInactiveProjectChange = viewModel::updateNotificationInactiveProject,
        onAssignmentReminderLeadMsChange = viewModel::updateAssignmentReminderLeadMs,
        onProjectInactivityDaysChange = viewModel::updateProjectInactivityThresholdDays,
        onShowResetConfirm = viewModel::showResetConfirmation,
        onDismissResetConfirm = viewModel::dismissResetConfirmation,
        onConfirmReset = viewModel::confirmReset,
        onExportBackup = {
            viewModel.exportBackup { json ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, json)
                    type = "application/json"
                }
                context.startActivity(Intent.createChooser(sendIntent, "Export Student OS Backup"))
            }
        },
        onPrepareImportBackup = viewModel::prepareImportBackup,
        onDismissImportConfirm = viewModel::dismissImportConfirmation,
        onConfirmImport = { viewModel.confirmImport() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onNavigateToAiDiagnostics: () -> Unit,
    onAttendanceThresholdChange: (Int) -> Unit,
    onOcrConfidenceChange: (Float) -> Unit,
    onAiEnabledChange: (Boolean) -> Unit,
    onAiProviderChange: (String) -> Unit,
    onDeepSeekApiKeyChange: (String) -> Unit,
    onAiIntradayUpdatesChange: (Boolean) -> Unit,
    onAiTonePreferenceChange: (String) -> Unit,
    onAiMaxCallsChange: (Int) -> Unit,
    onAiCacheMaxAgeChange: (Int) -> Unit,
    onCodeChefHandleChange: (String) -> Unit,
    onCodeforcesHandleChange: (String) -> Unit,
    onCpSyncIntervalChange: (Int) -> Unit,
    onShowTimePicker: () -> Unit,
    onDismissTimePicker: () -> Unit,
    onDailyBriefTimeChange: (String) -> Unit,
    onScoreWeightClassChange: (Int) -> Unit,
    onScoreWeightAssignmentChange: (Int) -> Unit,
    onScoreWeightProjectChange: (Int) -> Unit,
    onScoreWeightDsaChange: (Int) -> Unit,
    onNotificationDailyBriefChange: (Boolean) -> Unit,
    onNotificationAssignmentReminderChange: (Boolean) -> Unit,
    onNotificationClassReminderChange: (Boolean) -> Unit,
    onNotificationClassReminderLeadChange: (Int) -> Unit,
    onNotificationContestReminderChange: (Boolean) -> Unit,
    onNotificationFreeSlotChange: (Boolean) -> Unit,
    onNotificationInactiveProjectChange: (Boolean) -> Unit,
    onAssignmentReminderLeadMsChange: (Long) -> Unit,
    onProjectInactivityDaysChange: (Int) -> Unit,
    onShowResetConfirm: () -> Unit,
    onDismissResetConfirm: () -> Unit,
    onConfirmReset: () -> Unit,
    onExportBackup: () -> Unit,
    onPrepareImportBackup: (String) -> Unit,
    onDismissImportConfirm: () -> Unit,
    onConfirmImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editDialogKey by remember { mutableStateOf<String?>(null) }
    var importJsonText by remember { mutableStateOf("") }
    var showImportInputDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val s = uiState.settings

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── 1. Academic & Attendance ─────────────────────────────────────
            item {
                SettingsSection(
                    title = "Academic & Attendance",
                    icon = Icons.Default.DateRange,
                    subtitle = "Thresholds and OCR settings"
                ) {
                    SettingSliderRow(
                        title = "Target Attendance Threshold",
                        value = s.attendanceThreshold.toFloat(),
                        valueRange = 50f..100f,
                        valueDisplay = "${s.attendanceThreshold}%",
                        subtitle = "Subjects below this percentage will be highlighted in red",
                        onValueChange = { onAttendanceThresholdChange(it.toInt()) },
                        steps = 49
                    )
                    SettingSliderRow(
                        title = "OCR Confidence Threshold",
                        value = s.ocrConfidenceThreshold * 100f,
                        valueRange = 50f..100f,
                        valueDisplay = "${(s.ocrConfidenceThreshold * 100).toInt()}%",
                        subtitle = "Timetable fields below this confidence will be flagged in amber",
                        onValueChange = { onOcrConfidenceChange(it / 100f) },
                        steps = 49
                    )
                }
            }

            // ── 2. AI & Intelligence ─────────────────────────────────────────
            item {
                SettingsSection(
                    title = "Intelligence & AI",
                    icon = Icons.Default.Info,
                    subtitle = "DeepSeek LLM guidance and parameters"
                ) {
                    SettingSwitchRow(
                        title = "Enable AI Engine",
                        subtitle = "Uses DeepSeek LLM for personalized daily narrative",
                        checked = s.aiEnabled,
                        onCheckedChange = onAiEnabledChange
                    )

                    if (s.aiEnabled) {
                        SettingChoiceRow(
                            title = "AI Provider",
                            options = listOf("DEEPSEEK", "MOCK"),
                            selectedOption = s.aiProvider,
                            onOptionSelected = onAiProviderChange,
                            optionLabel = { if (it == "DEEPSEEK") "DeepSeek Cloud" else "Offline Mock" }
                        )

                        val maskedKey = if (s.deepSeekApiKey.isNotBlank()) {
                            "••••••••" + s.deepSeekApiKey.takeLast(4)
                        } else {
                            "Not configured (click to set)"
                        }
                        SettingActionRow(
                            title = "DeepSeek API Key",
                            valueText = maskedKey,
                            subtitle = "Stored securely on device",
                            onClick = { editDialogKey = "api_key" }
                        )

                        SettingSwitchRow(
                            title = "Intra-day Event Updates",
                            subtitle = "Automatically refresh guidance when tasks or attendance change",
                            checked = s.aiIntradayUpdatesEnabled,
                            onCheckedChange = onAiIntradayUpdatesChange
                        )

                        SettingChoiceRow(
                            title = "Guidance Tone Preference",
                            options = listOf("motivational", "concise", "neutral"),
                            selectedOption = s.aiTonePreference,
                            onOptionSelected = onAiTonePreferenceChange,
                            optionLabel = { it.replaceFirstChar { c -> c.uppercase() } }
                        )

                        SettingActionRow(
                            title = "Daily Maximum AI Calls",
                            valueText = "${s.aiMaxCallsPerDay} calls",
                            subtitle = "Prevents unexpected API quota usage",
                            onClick = { editDialogKey = "max_calls" }
                        )

                        SettingActionRow(
                            title = "Cache Max Age",
                            valueText = "${s.aiCacheMaxAgeHours} hours",
                            subtitle = "Reuses guidance cache when student state is identical",
                            onClick = { editDialogKey = "cache_age" }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigateToAiDiagnostics,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("View AI Diagnostics & Call Logs")
                    }
                }
            }

            // ── 3. Competitive Programming ───────────────────────────────────
            item {
                SettingsSection(
                    title = "Competitive Programming",
                    icon = Icons.Default.Build,
                    subtitle = "CodeChef & Codeforces handles"
                ) {
                    SettingActionRow(
                        title = "CodeChef Handle",
                        valueText = s.codeChefHandle.ifBlank { "Not set" },
                        onClick = { editDialogKey = "handle_codechef" }
                    )
                    SettingActionRow(
                        title = "Codeforces Handle",
                        valueText = s.codeforcesHandle.ifBlank { "Not set" },
                        onClick = { editDialogKey = "handle_codeforces" }
                    )

                    val syncHours = s.cpSyncIntervalMinutes / 60
                    SettingChoiceRow(
                        title = "Background Sync Interval",
                        options = listOf(60, 180, 360, 720, 1440),
                        selectedOption = s.cpSyncIntervalMinutes,
                        onOptionSelected = onCpSyncIntervalChange,
                        optionLabel = { "${it / 60}h" }
                    )
                }
            }

            // ── 4. Daily Brief & Scoring ─────────────────────────────────────
            item {
                SettingsSection(
                    title = "Daily Brief & Scoring",
                    icon = Icons.Default.DateRange,
                    subtitle = "Morning schedule and score weights"
                ) {
                    SettingActionRow(
                        title = "Daily Brief Time",
                        valueText = s.dailyBriefTimeHHmm,
                        subtitle = "Time when morning brief is generated",
                        onClick = onShowTimePicker
                    )

                    SettingActionRow(
                        title = "Class Score Weight",
                        valueText = "${s.scoreWeightClass} pts",
                        onClick = { editDialogKey = "weight_class" }
                    )
                    SettingActionRow(
                        title = "Assignment Score Weight",
                        valueText = "${s.scoreWeightAssignment} pts",
                        onClick = { editDialogKey = "weight_assignment" }
                    )
                    SettingActionRow(
                        title = "Project Action Score Weight",
                        valueText = "${s.scoreWeightProjectAction} pts",
                        onClick = { editDialogKey = "weight_project" }
                    )
                    SettingActionRow(
                        title = "DSA Topic Score Weight",
                        valueText = "${s.scoreWeightDsa} pts",
                        onClick = { editDialogKey = "weight_dsa" }
                    )
                }
            }

            // ── 5. Notification Preferences ─────────────────────────────────
            item {
                SettingsSection(
                    title = "Notifications",
                    icon = Icons.Default.Notifications,
                    subtitle = "Category alerts and lead times"
                ) {
                    SettingSwitchRow(
                        title = "Daily Brief Notification",
                        checked = s.notificationDailyBriefEnabled,
                        onCheckedChange = onNotificationDailyBriefChange
                    )
                    SettingSwitchRow(
                        title = "Class Reminders",
                        checked = s.notificationClassReminderEnabled,
                        onCheckedChange = onNotificationClassReminderChange
                    )
                    if (s.notificationClassReminderEnabled) {
                        SettingChoiceRow(
                            title = "Class Reminder Lead Time",
                            options = listOf(5, 10, 15, 30),
                            selectedOption = s.notificationClassReminderLeadMinutes,
                            onOptionSelected = onNotificationClassReminderLeadChange,
                            optionLabel = { "${it}m before" }
                        )
                    }
                    SettingSwitchRow(
                        title = "Assignment Reminders",
                        checked = s.notificationAssignmentReminderEnabled,
                        onCheckedChange = onNotificationAssignmentReminderChange
                    )
                    if (s.notificationAssignmentReminderEnabled) {
                        val leadHours = s.defaultAssignmentReminderLeadMs / 3600_000L
                        SettingChoiceRow(
                            title = "Assignment Reminder Lead",
                            options = listOf(6L * 3600_000L, 12L * 3600_000L, 24L * 3600_000L, 48L * 3600_000L),
                            selectedOption = s.defaultAssignmentReminderLeadMs,
                            onOptionSelected = onAssignmentReminderLeadMsChange,
                            optionLabel = { "${it / 3600_000L}h before" }
                        )
                    }
                    SettingSwitchRow(
                        title = "Contest Reminders",
                        checked = s.notificationContestReminderEnabled,
                        onCheckedChange = onNotificationContestReminderChange
                    )
                    SettingSwitchRow(
                        title = "Free Slot Recommendations",
                        checked = s.notificationFreeSlotEnabled,
                        onCheckedChange = onNotificationFreeSlotChange
                    )
                    SettingSwitchRow(
                        title = "Inactive Project Alerts",
                        checked = s.notificationInactiveProjectEnabled,
                        onCheckedChange = onNotificationInactiveProjectChange
                    )
                    if (s.notificationInactiveProjectEnabled) {
                        SettingChoiceRow(
                            title = "Project Inactivity Threshold",
                            options = listOf(3, 5, 7, 14),
                            selectedOption = s.projectInactivityThresholdDays,
                            onOptionSelected = onProjectInactivityDaysChange,
                            optionLabel = { "${it} days" }
                        )
                    }
                }
            }

            // ── 6. Data & Backup ─────────────────────────────────────────────
            item {
                SettingsSection(
                    title = "Data, Backup & Reset",
                    icon = Icons.Default.Share,
                    subtitle = "Export, import, or reset application data"
                ) {
                    OutlinedButton(
                        onClick = onExportBackup,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export Backup (JSON)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showImportInputDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Backup (JSON)")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onShowResetConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Reset All Settings to Defaults")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ── Edit Dialogs ────────────────────────────────────────────────────────
    when (editDialogKey) {
        "api_key" -> SettingEditDialog(
            title = "DeepSeek API Key",
            subtitle = "Enter your DeepSeek API key for AI-driven daily intelligence",
            initialValue = uiState.settings.deepSeekApiKey,
            placeholder = "sk-...",
            onDismiss = { editDialogKey = null },
            onConfirm = {
                onDeepSeekApiKeyChange(it)
                editDialogKey = null
            }
        )
        "handle_codechef" -> SettingEditDialog(
            title = "CodeChef Handle",
            initialValue = uiState.settings.codeChefHandle,
            placeholder = "e.g. tourist",
            onDismiss = { editDialogKey = null },
            onConfirm = {
                onCodeChefHandleChange(it)
                editDialogKey = null
            }
        )
        "handle_codeforces" -> SettingEditDialog(
            title = "Codeforces Handle",
            initialValue = uiState.settings.codeforcesHandle,
            placeholder = "e.g. tourist",
            onDismiss = { editDialogKey = null },
            onConfirm = {
                onCodeforcesHandleChange(it)
                editDialogKey = null
            }
        )
        "max_calls" -> SettingEditDialog(
            title = "Max AI Calls Per Day",
            initialValue = uiState.settings.aiMaxCallsPerDay.toString(),
            keyboardType = KeyboardType.Number,
            onDismiss = { editDialogKey = null },
            onConfirm = {
                it.toIntOrNull()?.let(onAiMaxCallsChange)
                editDialogKey = null
            }
        )
        "cache_age" -> SettingEditDialog(
            title = "Cache Max Age (Hours)",
            initialValue = uiState.settings.aiCacheMaxAgeHours.toString(),
            keyboardType = KeyboardType.Number,
            onDismiss = { editDialogKey = null },
            onConfirm = {
                it.toIntOrNull()?.let(onAiCacheMaxAgeChange)
                editDialogKey = null
            }
        )
        "weight_class" -> SettingEditDialog(
            title = "Class Score Weight",
            initialValue = uiState.settings.scoreWeightClass.toString(),
            keyboardType = KeyboardType.Number,
            onDismiss = { editDialogKey = null },
            onConfirm = {
                it.toIntOrNull()?.let(onScoreWeightClassChange)
                editDialogKey = null
            }
        )
        "weight_assignment" -> SettingEditDialog(
            title = "Assignment Score Weight",
            initialValue = uiState.settings.scoreWeightAssignment.toString(),
            keyboardType = KeyboardType.Number,
            onDismiss = { editDialogKey = null },
            onConfirm = {
                it.toIntOrNull()?.let(onScoreWeightAssignmentChange)
                editDialogKey = null
            }
        )
        "weight_project" -> SettingEditDialog(
            title = "Project Score Weight",
            initialValue = uiState.settings.scoreWeightProjectAction.toString(),
            keyboardType = KeyboardType.Number,
            onDismiss = { editDialogKey = null },
            onConfirm = {
                it.toIntOrNull()?.let(onScoreWeightProjectChange)
                editDialogKey = null
            }
        )
        "weight_dsa" -> SettingEditDialog(
            title = "DSA Score Weight",
            initialValue = uiState.settings.scoreWeightDsa.toString(),
            keyboardType = KeyboardType.Number,
            onDismiss = { editDialogKey = null },
            onConfirm = {
                it.toIntOrNull()?.let(onScoreWeightDsaChange)
                editDialogKey = null
            }
        )
    }

    // ── Time Picker Dialog ──────────────────────────────────────────────────
    if (uiState.showTimePickerDialog) {
        SettingTimePickerDialog(
            currentTime = uiState.settings.dailyBriefTimeHHmm,
            onDismiss = onDismissTimePicker,
            onConfirm = onDailyBriefTimeChange
        )
    }

    // ── Reset Confirmation Dialog ───────────────────────────────────────────
    if (uiState.showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissResetConfirm,
            title = { Text("Reset All Settings?") },
            text = { Text("All settings will be restored to their factory defaults. Your student data (classes, assignments, projects) will remain safe.") },
            confirmButton = {
                Button(
                    onClick = onConfirmReset,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissResetConfirm) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Import JSON Input Dialog ────────────────────────────────────────────
    if (showImportInputDialog) {
        AlertDialog(
            onDismissRequest = { showImportInputDialog = false },
            title = { Text("Import Backup JSON") },
            text = {
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    placeholder = { Text("Paste exported JSON backup here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    maxLines = 10
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            showImportInputDialog = false
                            onPrepareImportBackup(importJsonText)
                            importJsonText = ""
                        }
                    }
                ) {
                    Text("Validate & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Import Warning Confirmation Dialog ──────────────────────────────────
    if (uiState.showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissImportConfirm,
            title = { Text("Warning: Overwrite Existing Data?") },
            text = { Text("Restoring this backup will replace current database tables with the backup contents. This operation cannot be undone. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = onConfirmImport,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissImportConfirm) {
                    Text("Cancel")
                }
            }
        )
    }
}
