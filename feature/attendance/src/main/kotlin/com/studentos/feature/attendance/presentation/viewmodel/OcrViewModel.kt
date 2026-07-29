package com.studentos.feature.attendance.presentation.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.data.ocr.OcrProcessor
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import com.studentos.feature.attendance.domain.usecase.ImportTimetableUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OcrUiState {
    object Idle : OcrUiState
    object Loading : OcrUiState
    data class Content(
        val slots: List<ParsedTimetableSlot>,
        val hasWarnings: Boolean,
        val showReplaceDialog: Boolean = false,
        val errorMessage: String? = null
    ) : OcrUiState
    object ImportSuccess : OcrUiState
    data class Error(val message: String) : OcrUiState
}

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val ocrProcessor: OcrProcessor,
    private val importTimetableUseCase: ImportTimetableUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = OcrUiState.Loading
            val result = ocrProcessor.extract(bitmap)
            _uiState.value = OcrUiState.Content(
                slots = result.slots,
                hasWarnings = result.hasWarnings
            )
        }
    }

    fun updateSlot(index: Int, updatedSlot: ParsedTimetableSlot) {
        val currentState = _uiState.value as? OcrUiState.Content ?: return
        val updatedList = currentState.slots.toMutableList().apply {
            if (index in indices) {
                this[index] = updatedSlot
            }
        }
        val hasWarnings = updatedList.any { it.isLowConfidence }
        _uiState.value = currentState.copy(slots = updatedList, hasWarnings = hasWarnings)
    }

    fun removeSlot(index: Int) {
        val currentState = _uiState.value as? OcrUiState.Content ?: return
        val updatedList = currentState.slots.toMutableList().apply {
            if (index in indices) {
                removeAt(index)
            }
        }
        val hasWarnings = updatedList.any { it.isLowConfidence }
        _uiState.value = currentState.copy(slots = updatedList, hasWarnings = hasWarnings)
    }

    fun confirmImport(replaceExisting: Boolean = false) {
        val currentState = _uiState.value as? OcrUiState.Content ?: return
        viewModelScope.launch {
            val result = importTimetableUseCase(
                slots = currentState.slots,
                replaceExisting = replaceExisting
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.value = OcrUiState.ImportSuccess
                }
                is AppResult.Failure -> {
                    if (!replaceExisting) {
                        _uiState.value = currentState.copy(showReplaceDialog = true)
                    } else {
                        _uiState.value = OcrUiState.Error("Import Failed")
                    }
                }
            }
        }
    }

    fun dismissReplaceDialog() {
        val currentState = _uiState.value as? OcrUiState.Content ?: return
        _uiState.value = currentState.copy(showReplaceDialog = false)
    }
}
