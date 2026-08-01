package com.studentos.feature.intelligence.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.intelligence.domain.usecase.GetBriefHistoryUseCase
import com.studentos.feature.intelligence.presentation.state.DailyBriefHistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyBriefHistoryViewModel @Inject constructor(
    private val getBriefHistoryUseCase: GetBriefHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyBriefHistoryUiState())
    val uiState: StateFlow<DailyBriefHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            getBriefHistoryUseCase()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load history"
                        )
                    }
                }
                .collect { list ->
                    val sorted = list.sortedByDescending { it.date }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEmpty = sorted.isEmpty(),
                            history = sorted,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
