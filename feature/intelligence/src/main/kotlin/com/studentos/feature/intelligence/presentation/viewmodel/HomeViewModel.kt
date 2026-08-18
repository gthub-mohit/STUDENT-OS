package com.studentos.feature.intelligence.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.intelligence.domain.model.TodayFocusItem
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import com.studentos.feature.intelligence.domain.usecase.GetComingUpUseCase
import com.studentos.feature.intelligence.domain.usecase.GetTodayFocusUseCase
import com.studentos.feature.intelligence.domain.usecase.ToggleFocusItemUseCase
import com.studentos.feature.intelligence.orchestrator.IntelligenceOrchestrator
import com.studentos.feature.intelligence.presentation.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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
class HomeViewModel @Inject constructor(
    private val getTodayFocusUseCase: GetTodayFocusUseCase,
    private val getComingUpUseCase: GetComingUpUseCase,
    private val toggleFocusItemUseCase: ToggleFocusItemUseCase,
    private val dailyBriefRepository: DailyBriefRepository,
    private val orchestrator: IntelligenceOrchestrator,
    private val appEventBus: AppEventBus,
    private val clock: Clock
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val todayDateString = LocalDate.now(clock).format(dateFormatter)

    private val _isGenerating = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var eventSubscriptionJob: Job? = null
    private var dataObservationJob: Job? = null

    init {
        startObservingData()
        startEventSubscription()
    }

    private fun startObservingData() {
        dataObservationJob?.cancel()
        dataObservationJob = viewModelScope.launch {
            combine(
                getTodayFocusUseCase(),
                getComingUpUseCase(),
                dailyBriefRepository.getBriefForDate(todayDateString),
                _isGenerating,
                _errorMessage
            ) { focusItems, comingUpItems, dailyBrief, isGenerating, errorMessage ->
                val total = focusItems.size
                val completed = focusItems.count { it.isCompleted }
                val progress = if (total > 0) (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                val hasBrief = dailyBrief != null

                val goalSummary = when {
                    isGenerating -> "Generating today's brief…"
                    total == 0 -> "Tap to view Daily Brief"
                    completed == total -> "All priorities completed today"
                    else -> "Tap to see today's plan"
                }

                HomeUiState(
                    isLoading = false,
                    isGenerating = isGenerating,
                    hasBrief = hasBrief,
                    completedPrioritiesCount = completed,
                    totalPrioritiesCount = total,
                    progressBarValue = progress,
                    todayGoalSummary = goalSummary,
                    focusItems = focusItems,
                    comingUpItems = comingUpItems,
                    errorMessage = errorMessage
                )
            }.catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load home overview"
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleFocusItem(item: TodayFocusItem) {
        viewModelScope.launch {
            try {
                toggleFocusItemUseCase(item)
            } catch (e: Exception) {
                _errorMessage.update { e.localizedMessage ?: "Failed to update item" }
            }
        }
    }

    fun generateTodayBrief() {
        val todayLocalDate = LocalDate.now(clock)
        _isGenerating.update { true }
        _errorMessage.update { null }

        viewModelScope.launch {
            try {
                orchestrator.generateMorningBrief(todayLocalDate)
            } catch (e: Exception) {
                _errorMessage.update { e.localizedMessage ?: "Generation failed" }
            } finally {
                _isGenerating.update { false }
            }
        }
    }

    fun startEventSubscription(debounceMillis: Long = 10_000L) {
        eventSubscriptionJob?.cancel()
        eventSubscriptionJob = appEventBus.events
            .debounce(debounceMillis)
            .onEach { event ->
                if (isRelevantEvent(event)) {
                    _errorMessage.update { null }
                }
            }
            .launchIn(viewModelScope)
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
