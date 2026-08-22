package com.studentos.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.notifications.scheduler.NotificationRescheduler
import com.studentos.feature.settings.domain.repository.BackupRepository
import com.studentos.feature.settings.domain.repository.SettingsRepository
import com.studentos.feature.settings.presentation.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val notificationRescheduler: NotificationRescheduler? = null
) : ViewModel() {

    private val _uiFlags = MutableStateFlow(Flags())

    private data class Flags(
        val isApiKeyVisible: Boolean = false,
        val userMessage: String? = null,
        val showResetConfirmDialog: Boolean = false,
        val showImportConfirmDialog: Boolean = false,
        val pendingImportJson: String? = null,
        val showTimePickerDialog: Boolean = false
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeAllSettings(),
        _uiFlags
    ) { settings, flags ->
        SettingsUiState(
            isLoading = false,
            settings = settings,
            isApiKeyVisible = flags.isApiKeyVisible,
            userMessage = flags.userMessage,
            showResetConfirmDialog = flags.showResetConfirmDialog,
            showImportConfirmDialog = flags.showImportConfirmDialog,
            pendingImportJson = flags.pendingImportJson,
            showTimePickerDialog = flags.showTimePickerDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState(isLoading = true)
    )

    fun toggleApiKeyVisibility() {
        _uiFlags.update { it.copy(isApiKeyVisible = !it.isApiKeyVisible) }
    }

    fun clearUserMessage() {
        _uiFlags.update { it.copy(userMessage = null) }
    }

    // ── Academic & Attendance ───────────────────────────────────────────────
    fun updateAttendanceThreshold(threshold: Int) {
        viewModelScope.launch {
            settingsRepository.setAttendanceThreshold(threshold.coerceIn(1, 100))
        }
    }

    fun updateOcrConfidenceThreshold(threshold: Float) {
        viewModelScope.launch {
            settingsRepository.setOcrConfidenceThreshold(threshold.coerceIn(0.5f, 1.0f))
        }
    }

    // ── AI & Intelligence ───────────────────────────────────────────────────
    fun updateAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiEnabled(enabled)
        }
    }

    fun updateAiProvider(provider: String) {
        viewModelScope.launch {
            settingsRepository.setAiProvider(provider)
        }
    }

    fun updateDeepSeekApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.setDeepSeekApiKey(apiKey.trim())
            _uiFlags.update { it.copy(userMessage = "API key saved successfully") }
        }
    }

    fun updateAiIntradayUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiIntradayUpdatesEnabled(enabled)
        }
    }

    fun updateAiTonePreference(tone: String) {
        viewModelScope.launch {
            settingsRepository.setAiTonePreference(tone)
        }
    }

    fun updateAiMaxCallsPerDay(maxCalls: Int) {
        viewModelScope.launch {
            settingsRepository.setAiMaxCallsPerDay(maxCalls.coerceAtLeast(1))
        }
    }

    fun updateAiCacheMaxAgeHours(hours: Int) {
        viewModelScope.launch {
            settingsRepository.setAiCacheMaxAgeHours(hours.coerceAtLeast(1))
        }
    }

    // ── Competitive Programming ─────────────────────────────────────────────
    fun updateCodeChefHandle(handle: String) {
        viewModelScope.launch {
            settingsRepository.setCodeChefHandle(handle.trim())
        }
    }

    fun updateCodeforcesHandle(handle: String) {
        viewModelScope.launch {
            settingsRepository.setCodeforcesHandle(handle.trim())
        }
    }

    fun updateCpSyncInterval(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setCpSyncIntervalMinutes(minutes.coerceAtLeast(15))
        }
    }

    // ── Daily Brief & Scoring ───────────────────────────────────────────────
    fun updateDailyBriefTime(hhmm: String) {
        viewModelScope.launch {
            settingsRepository.setDailyBriefTimeHHmm(hhmm)
            _uiFlags.update { it.copy(showTimePickerDialog = false) }
        }
    }

    fun showTimePickerDialog() {
        _uiFlags.update { it.copy(showTimePickerDialog = true) }
    }

    fun dismissTimePickerDialog() {
        _uiFlags.update { it.copy(showTimePickerDialog = false) }
    }

    fun updateScoreWeightClass(weight: Int) {
        viewModelScope.launch {
            settingsRepository.setScoreWeightClass(weight.coerceAtLeast(0))
        }
    }

    fun updateScoreWeightAssignment(weight: Int) {
        viewModelScope.launch {
            settingsRepository.setScoreWeightAssignment(weight.coerceAtLeast(0))
        }
    }

    fun updateScoreWeightProjectAction(weight: Int) {
        viewModelScope.launch {
            settingsRepository.setScoreWeightProjectAction(weight.coerceAtLeast(0))
        }
    }

    fun updateScoreWeightDsa(weight: Int) {
        viewModelScope.launch {
            settingsRepository.setScoreWeightDsa(weight.coerceAtLeast(0))
        }
    }

    // ── Notifications ───────────────────────────────────────────────────────
    fun updateNotificationDailyBrief(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationDailyBriefEnabled(enabled)
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    fun updateNotificationAssignmentReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationAssignmentReminderEnabled(enabled)
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    fun updateNotificationClassReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationClassReminderEnabled(enabled)
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    fun updateNotificationClassReminderLead(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setNotificationClassReminderLeadMinutes(minutes.coerceAtLeast(1))
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    fun updateNotificationContestReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationContestReminderEnabled(enabled)
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    fun updateNotificationFreeSlot(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationFreeSlotEnabled(enabled)
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    fun updateNotificationInactiveProject(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationInactiveProjectEnabled(enabled)
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    fun updateAssignmentReminderLeadMs(leadMs: Long) {
        viewModelScope.launch {
            settingsRepository.setDefaultAssignmentReminderLeadMs(leadMs)
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    fun updateContestReminderLookaheadMs(lookaheadMs: Long) {
        viewModelScope.launch {
            settingsRepository.setContestReminderLookaheadMs(lookaheadMs)
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    fun updateProjectInactivityThresholdDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setProjectInactivityThresholdDays(days.coerceAtLeast(1))
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
        }
    }

    // ── Reset to Defaults ───────────────────────────────────────────────────
    fun showResetConfirmation() {
        _uiFlags.update { it.copy(showResetConfirmDialog = true) }
    }

    fun dismissResetConfirmation() {
        _uiFlags.update { it.copy(showResetConfirmDialog = false) }
    }

    fun confirmReset() {
        viewModelScope.launch {
            settingsRepository.reset()
            try { notificationRescheduler?.rescheduleAll() } catch (_: Exception) {}
            _uiFlags.update {
                it.copy(
                    showResetConfirmDialog = false,
                    userMessage = "All settings reset to defaults"
                )
            }
        }
    }

    // ── Backup & Restore ────────────────────────────────────────────────────
    fun exportBackup(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = backupRepository.exportBackupJson()
                onExportReady(json)
                _uiFlags.update { it.copy(userMessage = "Backup generated successfully") }
            } catch (e: Exception) {
                _uiFlags.update { it.copy(userMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun prepareImportBackup(jsonString: String) {
        _uiFlags.update {
            it.copy(
                showImportConfirmDialog = true,
                pendingImportJson = jsonString
            )
        }
    }

    fun dismissImportConfirmation() {
        _uiFlags.update {
            it.copy(
                showImportConfirmDialog = false,
                pendingImportJson = null
            )
        }
    }

    fun confirmImport(onSuccess: () -> Unit = {}) {
        val jsonToImport = _uiFlags.value.pendingImportJson ?: return
        viewModelScope.launch {
            val result = backupRepository.restoreBackupJson(jsonToImport)
            if (result.isSuccess) {
                _uiFlags.update {
                    it.copy(
                        showImportConfirmDialog = false,
                        pendingImportJson = null,
                        userMessage = "Backup restored successfully"
                    )
                }
                onSuccess()
            } else {
                _uiFlags.update {
                    it.copy(
                        showImportConfirmDialog = false,
                        pendingImportJson = null,
                        userMessage = "Restore failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }
}
