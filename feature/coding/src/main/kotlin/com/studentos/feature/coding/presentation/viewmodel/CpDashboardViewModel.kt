package com.studentos.feature.coding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.coding.domain.repository.CpRepository
import com.studentos.feature.coding.domain.usecase.GetGroupedContestsUseCase
import com.studentos.feature.coding.presentation.state.CpDashboardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CpDashboardViewModel @Inject constructor(
    private val cpRepository: CpRepository,
    private val getGroupedContestsUseCase: GetGroupedContestsUseCase
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _syncError = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(CpDashboardUiState(isLoading = true))
    val uiState: StateFlow<CpDashboardUiState> = _uiState.asStateFlow()

    init {
        observeProfilesAndContests()
    }

    private fun observeProfilesAndContests() {
        viewModelScope.launch {
            combine(
                cpRepository.getProfiles(),
                cpRepository.getAllContests(),
                getGroupedContestsUseCase(),
                _isSyncing,
                _syncError
            ) { profiles, contests, groupedContests, syncing, error ->
                val latestSyncTime = profiles.mapNotNull { it.lastSyncedAt }.maxOrNull()
                CpDashboardUiState(
                    isLoading = false,
                    profiles = profiles,
                    contests = contests,
                    groupedContests = groupedContests,
                    lastSyncedAt = latestSyncTime,
                    isOffline = error != null,
                    errorMessage = error
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            when (val result = cpRepository.syncProfiles()) {
                is AppResult.Success -> {
                    _syncError.value = null
                }
                is AppResult.Failure -> {
                    _syncError.value = formatAppError(result.reason)
                }
            }
            _isSyncing.value = false
        }
    }

    fun addOrUpdateProfile(platform: String, handle: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            when (val result = cpRepository.addOrUpdateProfile(platform, handle)) {
                is AppResult.Success -> {
                    _syncError.value = null
                }
                is AppResult.Failure -> {
                    _syncError.value = formatAppError(result.reason)
                }
            }
            _isSyncing.value = false
        }
    }

    fun clearError() {
        _syncError.value = null
    }

    private fun formatAppError(error: AppError): String {
        return when (error) {
            is AppError.DatabaseError -> error.message
            is AppError.NetworkError -> error.message
            is AppError.ValidationError -> error.message
            AppError.Offline -> "Device is offline"
            AppError.RateLimited -> "Rate limit reached"
        }
    }
}
