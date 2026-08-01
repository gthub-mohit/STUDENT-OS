package com.studentos.feature.intelligence.presentation.state

import com.studentos.feature.intelligence.domain.model.DailyBriefSummaryDomain

data class DailyBriefHistoryUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val history: List<DailyBriefSummaryDomain> = emptyList(),
    val errorMessage: String? = null
)
