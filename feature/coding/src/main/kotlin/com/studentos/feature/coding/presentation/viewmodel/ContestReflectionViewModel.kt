package com.studentos.feature.coding.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.coding.domain.model.CpReflection
import com.studentos.feature.coding.domain.repository.CpRepository
import com.studentos.feature.coding.domain.usecase.SaveContestReflectionUseCase
import com.studentos.feature.coding.presentation.state.ContestReflectionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContestReflectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cpRepository: CpRepository,
    private val saveContestReflectionUseCase: SaveContestReflectionUseCase
) : ViewModel() {

    private val contestId: Long = checkNotNull(savedStateHandle["contestId"]) {
        "contestId parameter is required for ContestReflectionViewModel"
    }.toString().toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(ContestReflectionUiState(contestId = contestId))
    val uiState: StateFlow<ContestReflectionUiState> = _uiState.asStateFlow()

    private var initialWentWrong = ""
    private var initialToRevise = ""
    private var initialSelfRating = 3

    init {
        loadExistingReflection()
    }

    private fun loadExistingReflection() {
        if (contestId <= 0L) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            cpRepository.getReflection(contestId).collect { existing ->
                if (existing != null) {
                    initialWentWrong = existing.wentWrong ?: ""
                    initialToRevise = existing.toRevise ?: ""
                    initialSelfRating = existing.selfRating.coerceIn(1, 5)

                    _uiState.update { current ->
                        current.copy(
                            existingReflectionId = existing.id,
                            wentWrong = initialWentWrong,
                            toRevise = initialToRevise,
                            selfRating = initialSelfRating,
                            isLoading = false,
                            hasUnsavedChanges = false
                        )
                    }
                } else {
                    _uiState.update { current -> current.copy(isLoading = false) }
                }
            }
        }
    }

    fun onWentWrongChanged(text: String) {
        _uiState.update { current ->
            val hasChanged = text != initialWentWrong || current.toRevise != initialToRevise || current.selfRating != initialSelfRating
            current.copy(wentWrong = text, hasUnsavedChanges = hasChanged)
        }
    }

    fun onToReviseChanged(text: String) {
        _uiState.update { current ->
            val hasChanged = current.wentWrong != initialWentWrong || text != initialToRevise || current.selfRating != initialSelfRating
            current.copy(toRevise = text, hasUnsavedChanges = hasChanged)
        }
    }

    fun onSelfRatingChanged(rating: Int) {
        val validRating = rating.coerceIn(1, 5)
        _uiState.update { current ->
            val hasChanged = current.wentWrong != initialWentWrong || current.toRevise != initialToRevise || validRating != initialSelfRating
            current.copy(selfRating = validRating, hasUnsavedChanges = hasChanged)
        }
    }

    fun onSaveClicked() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val reflection = CpReflection(
                id = currentState.existingReflectionId,
                contestId = currentState.contestId,
                wentWrong = currentState.wentWrong,
                toRevise = currentState.toRevise,
                selfRating = currentState.selfRating
            )

            try {
                saveContestReflectionUseCase(reflection)
                initialWentWrong = currentState.wentWrong
                initialToRevise = currentState.toRevise
                initialSelfRating = currentState.selfRating

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        isSaved = true,
                        hasUnsavedChanges = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to save reflection"
                    )
                }
            }
        }
    }

    fun onBackClicked(onNavigateBack: () -> Unit) {
        if (_uiState.value.hasUnsavedChanges) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            onNavigateBack()
        }
    }

    fun onConfirmDiscard(onNavigateBack: () -> Unit) {
        _uiState.update { it.copy(showDiscardDialog = false, hasUnsavedChanges = false) }
        onNavigateBack()
    }

    fun onDismissDiscardDialog() {
        _uiState.update { it.copy(showDiscardDialog = false) }
    }
}
