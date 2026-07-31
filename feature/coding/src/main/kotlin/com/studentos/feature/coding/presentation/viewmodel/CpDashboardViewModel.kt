package com.studentos.feature.coding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.coding.domain.repository.CpRepository
import com.studentos.feature.coding.presentation.state.CpDashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CpDashboardViewModel @Inject constructor(
    cpRepository: CpRepository
) : ViewModel() {

    val uiState: StateFlow<CpDashboardUiState> = combine(
        cpRepository.getProfiles(),
        cpRepository.getAllContests()
    ) { profiles, contests ->
        val latestSyncTime = profiles.mapNotNull { it.lastSyncedAt }.maxOrNull()

        CpDashboardUiState(
            isLoading = false,
            profiles = profiles,
            contests = contests,
            lastSyncedAt = latestSyncTime,
            isOffline = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CpDashboardUiState(isLoading = true)
    )
}
