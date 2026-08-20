package com.studentos.feature.settings.presentation.state

import com.studentos.core.database.entity.AiCallLogEntity

data class AiDiagnosticsUiState(
    val isLoading: Boolean = true,
    val logs: List<AiCallLogEntity> = emptyList(),
    val callsToday: Int = 0,
    val maxCallsPerDay: Int = 10,
    val tokensToday: Int = 0,
    val estimatedCostTodayUsd: Double = 0.0,
    val successRatePercent: Float = 100f,
    val aiProvider: String = "DEEPSEEK"
)
