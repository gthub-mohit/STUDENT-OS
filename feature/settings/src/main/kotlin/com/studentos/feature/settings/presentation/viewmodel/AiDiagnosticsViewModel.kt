package com.studentos.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.dao.AiCallLogDao
import com.studentos.feature.settings.domain.repository.SettingsRepository
import com.studentos.feature.settings.presentation.state.AiDiagnosticsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AiDiagnosticsViewModel @Inject constructor(
    private val aiCallLogDao: AiCallLogDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val startOfDayEpochMs: Long
        get() {
            return LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

    val uiState: StateFlow<AiDiagnosticsUiState> = combine(
        aiCallLogDao.getRecentLogs(50),
        settingsRepository.observeAllSettings()
    ) { logs, settings ->
        val todayLogs = logs.filter { it.createdAt >= startOfDayEpochMs }
        val callsToday = todayLogs.size
        val tokensToday = todayLogs.sumOf { it.tokenCount }
        val successfulCalls = todayLogs.count { it.success }
        val successRate = if (callsToday > 0) (successfulCalls.toFloat() / callsToday) * 100f else 100f

        // Estimated cost based on standard DeepSeek chat completions: ~$0.0002 per 1K tokens
        val estimatedCost = (tokensToday.toDouble() / 1000.0) * 0.0002

        AiDiagnosticsUiState(
            isLoading = false,
            logs = logs,
            callsToday = callsToday,
            maxCallsPerDay = settings.aiMaxCallsPerDay,
            tokensToday = tokensToday,
            estimatedCostTodayUsd = estimatedCost,
            successRatePercent = successRate,
            aiProvider = settings.aiProvider
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AiDiagnosticsUiState(isLoading = true)
    )
}
