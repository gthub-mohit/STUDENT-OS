package com.studentos.feature.intelligence.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import com.studentos.feature.intelligence.presentation.state.DailyScoreUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class DailyScoreViewModel @Inject constructor(
    private val repository: DailyBriefRepository,
    private val appEventBus: AppEventBus,
    private val clock: Clock
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _uiState = MutableStateFlow(DailyScoreUiState())
    val uiState: StateFlow<DailyScoreUiState> = _uiState.asStateFlow()

    private var eventSubscriptionJob: Job? = null

    init {
        loadScoreForToday()
        startEventSubscription()
    }

    fun loadScoreForToday() {
        val today = LocalDate.now(clock).format(dateFormatter)
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            repository.getBriefForDate(today)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load score"
                        )
                    }
                }
                .collect { brief ->
                    updateScoreState(brief)
                }
        }
    }

    fun startEventSubscription(debounceMillis: Long = 30_000L) {
        eventSubscriptionJob?.cancel()
        eventSubscriptionJob = appEventBus.events
            .debounce(debounceMillis)
            .onEach { event ->
                if (isRelevantEvent(event)) {
                    refreshScoreOnly()
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshScoreOnly() {
        val today = LocalDate.now(clock).format(dateFormatter)
        val brief = repository.getBriefForDate(today).firstOrNull()
        updateScoreState(brief)
    }

    private fun updateScoreState(brief: DailyBrief?) {
        if (brief == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    targetScore = 0,
                    currentScore = 0,
                    progressPercentage = 0f,
                    progressBarValue = 0f,
                    remainingScore = 0,
                    errorMessage = null
                )
            }
            return
        }

        val target = brief.scoreTarget
        val actual = brief.scoreActual
        val progressPct = if (target > 0) (actual.toFloat() / target.toFloat() * 100f).coerceIn(0f, 100f) else 0f
        val progressVal = if (target > 0) (actual.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
        val remaining = (target - actual).coerceAtLeast(0)

        _uiState.update { current ->
            if (current.targetScore == target && current.currentScore == actual && !current.isLoading) {
                current
            } else {
                current.copy(
                    isLoading = false,
                    targetScore = target,
                    currentScore = actual,
                    progressPercentage = progressPct,
                    progressBarValue = progressVal,
                    remainingScore = remaining,
                    errorMessage = null
                )
            }
        }
    }

    private fun isRelevantEvent(event: AppEvent): Boolean {
        return when (event) {
            is AppEvent.AttendanceMarked,
            is AppEvent.AttendanceUpdated,
            is AppEvent.AssignmentStatusChanged,
            is AppEvent.AssignmentCreated,
            is AppEvent.AssignmentDeleted,
            is AppEvent.ProjectTaskCompleted,
            is AppEvent.ProjectUpdated,
            is AppEvent.CpSyncCompleted,
            is AppEvent.ContestReflectionAdded,
            is AppEvent.DsaTopicUpdated,
            is AppEvent.DailyScoreChanged -> true
        }
    }
}
