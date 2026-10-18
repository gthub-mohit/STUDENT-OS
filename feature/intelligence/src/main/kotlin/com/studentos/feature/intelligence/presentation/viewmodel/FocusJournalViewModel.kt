package com.studentos.feature.intelligence.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.dao.ActivityLogDao
import com.studentos.core.database.dao.CommitmentDao
import com.studentos.core.database.dao.GoalDao
import com.studentos.core.database.dao.JournalDao
import com.studentos.core.database.entity.JournalEntryEntity
import com.studentos.feature.intelligence.domain.model.EditableActivity
import com.studentos.feature.intelligence.domain.model.EditableCommitment
import com.studentos.feature.intelligence.domain.model.ExtractionResult
import com.studentos.feature.intelligence.domain.usecase.ConfirmAndPersistActivitiesUseCase
import com.studentos.feature.intelligence.domain.usecase.ExtractJournalUseCase
import com.studentos.feature.intelligence.presentation.state.ExtractionPreview
import com.studentos.feature.intelligence.presentation.state.FocusJournalUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class FocusJournalViewModel @Inject constructor(
    private val extractJournalUseCase: ExtractJournalUseCase,
    private val confirmAndPersistActivitiesUseCase: ConfirmAndPersistActivitiesUseCase,
    private val activityLogDao: ActivityLogDao,
    private val commitmentDao: CommitmentDao,
    private val journalDao: JournalDao,
    private val goalDao: GoalDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusJournalUiState())
    val uiState: StateFlow<FocusJournalUiState> = _uiState.asStateFlow()

    init {
        observeDatabaseData()
    }

    private fun observeDatabaseData() {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 1. Observe today's confirmed activity logs
        activityLogDao.observeLogsBetween(startOfDay, endOfDay)
            .onEach { logs ->
                _uiState.update { it.copy(todayActivities = logs) }
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = "Failed to load activities: ${e.message}") }
            }
            .launchIn(viewModelScope)

        // 2. Observe pending commitments
        commitmentDao.observePendingCommitments()
            .onEach { commitments ->
                _uiState.update { it.copy(pendingCommitments = commitments) }
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = "Failed to load commitments: ${e.message}") }
            }
            .launchIn(viewModelScope)

        // 3. Observe journal history
        journalDao.observeAllEntries()
            .onEach { entries ->
                _uiState.update { it.copy(recentJournalEntries = entries) }
            }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = "Failed to load journal entries: ${e.message}") }
            }
            .launchIn(viewModelScope)

        // 4. Observe active goals for context & mapping
        goalDao.observeActiveGoals()
            .onEach { goals ->
                _uiState.update { it.copy(activeGoals = goals) }
            }
            .catch { /* non-fatal */ }
            .launchIn(viewModelScope)
    }

    fun onRawInputChanged(input: String) {
        _uiState.update { it.copy(rawInput = input) }
    }

    fun submitForExtraction() {
        val currentInput = _uiState.value.rawInput.trim()
        if (currentInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter some notes before extracting.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    errorMessage = null,
                    userMessage = null
                )
            }

            when (val result = extractJournalUseCase(currentInput)) {
                is ExtractionResult.Success -> {
                    val editableActivities = result.response.activities.map { dto ->
                        EditableActivity(
                            title = dto.title,
                            activityType = dto.activityType,
                            metricDelta = dto.metricDelta,
                            durationMinutes = dto.durationMinutes,
                            matchedGoalId = dto.matchedGoalId,
                            confidence = dto.confidence
                        )
                    }

                    val editableCommitments = result.response.commitments.map { dto ->
                        EditableCommitment(
                            title = dto.title,
                            category = dto.category,
                            targetDate = dto.targetDate,
                            targetTime = dto.targetTime,
                            priority = dto.priority ?: "MEDIUM",
                            estimatedMinutes = dto.estimatedMinutes
                        )
                    }

                    _uiState.update { state ->
                        state.copy(
                            isExtracting = false,
                            rawInput = "",
                            preview = ExtractionPreview(
                                journalEntryId = result.journalEntryId,
                                summary = result.response.summary ?: "Journal log processed.",
                                activities = editableActivities,
                                commitments = editableCommitments,
                                clarifications = result.response.clarifications,
                                originalInput = currentInput
                            ),
                            userMessage = "AI extracted items. Please review and confirm before saving."
                        )
                    }
                }
                is ExtractionResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isExtracting = false,
                            errorMessage = result.message,
                            isOfflineMode = result.isOffline
                        )
                    }
                }
            }
        }
    }

    fun saveRawNoteOnly() {
        val currentInput = _uiState.value.rawInput.trim()
        if (currentInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Note cannot be empty.") }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val todayStr = LocalDate.now().toString()
            try {
                journalDao.insert(
                    JournalEntryEntity(
                        date = todayStr,
                        rawLog = currentInput,
                        structuredSummary = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                _uiState.update {
                    it.copy(
                        rawInput = "",
                        userMessage = "Journal note saved locally.",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save note: ${e.message}") }
            }
        }
    }

    // ── Preview editing ────────────────────────────────────────────────────────

    fun updateActivity(tempId: String, update: (EditableActivity) -> EditableActivity) {
        _uiState.update { state ->
            val preview = state.preview ?: return@update state
            val updated = preview.activities.map {
                if (it.tempId == tempId) update(it) else it
            }
            state.copy(preview = preview.copy(activities = updated))
        }
    }

    fun removeActivity(tempId: String) {
        _uiState.update { state ->
            val preview = state.preview ?: return@update state
            state.copy(preview = preview.copy(activities = preview.activities.filterNot { it.tempId == tempId }))
        }
    }

    fun addEmptyActivity() {
        _uiState.update { state ->
            val preview = state.preview ?: return@update state
            val newActivity = EditableActivity(
                title = "",
                activityType = "GENERAL",
                metricDelta = 1.0,
                durationMinutes = null,
                matchedGoalId = null
            )
            state.copy(preview = preview.copy(activities = preview.activities + newActivity))
        }
    }

    fun updateCommitment(tempId: String, update: (EditableCommitment) -> EditableCommitment) {
        _uiState.update { state ->
            val preview = state.preview ?: return@update state
            val updated = preview.commitments.map {
                if (it.tempId == tempId) update(it) else it
            }
            state.copy(preview = preview.copy(commitments = updated))
        }
    }

    fun removeCommitment(tempId: String) {
        _uiState.update { state ->
            val preview = state.preview ?: return@update state
            state.copy(preview = preview.copy(commitments = preview.commitments.filterNot { it.tempId == tempId }))
        }
    }

    fun addEmptyCommitment() {
        _uiState.update { state ->
            val preview = state.preview ?: return@update state
            val newCommitment = EditableCommitment(
                title = "",
                category = "ACADEMIC",
                priority = "MEDIUM"
            )
            state.copy(preview = preview.copy(commitments = preview.commitments + newCommitment))
        }
    }

    // ── Confirmation & Gating ──────────────────────────────────────────────────

    fun confirmPreview() {
        val preview = _uiState.value.preview ?: return
        viewModelScope.launch {
            try {
                val result = confirmAndPersistActivitiesUseCase(
                    journalEntryId = preview.journalEntryId,
                    confirmedActivities = preview.activities,
                    confirmedCommitments = preview.commitments,
                    summary = preview.summary,
                    rawInput = preview.originalInput
                )

                _uiState.update { state ->
                    state.copy(
                        preview = null,
                        userMessage = "Persisted ${result.persistedActivitiesCount} activities and ${result.persistedCommitmentsCount} commitments." +
                                if (result.skippedDuplicatesCount > 0) " (${result.skippedDuplicatesCount} duplicates skipped)" else "",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to persist records: ${e.message}") }
            }
        }
    }

    fun discardPreview() {
        _uiState.update { state ->
            state.copy(
                preview = null,
                userMessage = "Preview discarded. Your original journal note was preserved in local history.",
                errorMessage = null
            )
        }
    }

    // ── Commitments actions ────────────────────────────────────────────────────

    fun toggleCommitmentComplete(commitmentId: Long) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                commitmentDao.updateStatus(commitmentId, "COMPLETED", now)
                _uiState.update { it.copy(userMessage = "Commitment marked completed!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Could not update commitment: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(userMessage = null, errorMessage = null) }
    }
}
