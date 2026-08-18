package com.studentos.feature.attendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import com.studentos.feature.attendance.domain.repository.TimetableRepository
import com.studentos.feature.attendance.presentation.state.EditTimetableUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTimetableViewModel @Inject constructor(
    private val timetableRepository: TimetableRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditTimetableUiState>(EditTimetableUiState.Loading)
    val uiState: StateFlow<EditTimetableUiState> = _uiState.asStateFlow()

    private val _selectedDay = MutableStateFlow(1) // 1 = Monday

    init {
        loadTimetableData()
    }

    private fun loadTimetableData() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                timetableRepository.getAllSlots(),
                subjectRepository.getActiveSubjects(),
                _selectedDay
            ) { slots, subjects, selectedDay ->
                EditTimetableUiState.Success(
                    slots = slots,
                    subjects = subjects,
                    selectedDayOfWeek = selectedDay
                )
            }.catch { error ->
                _uiState.value = EditTimetableUiState.Error(error.message ?: "Failed to load timetable")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectDay(dayOfWeek: Int) {
        _selectedDay.value = dayOfWeek
    }

    fun addSlot(
        subjectId: Long,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        location: String?,
        weekParity: String?,
        validFrom: Long = System.currentTimeMillis(),
        validUntil: Long? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newSlot = TimetableSlotEntity(
                subjectId = subjectId,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                location = location,
                weekParity = weekParity,
                validFrom = validFrom,
                validUntil = validUntil
            )
            when (val result = timetableRepository.addSlot(newSlot)) {
                is AppResult.Failure -> {
                    val msg = when (val r = result.reason) {
                        is com.studentos.core.events.AppError.DatabaseError -> r.message
                        is com.studentos.core.events.AppError.ValidationError -> r.message
                        is com.studentos.core.events.AppError.NetworkError -> r.message
                        else -> "Failed to add timetable slot"
                    }
                    _uiState.value = EditTimetableUiState.Error(msg)
                }
                is AppResult.Success -> {
                    // Reactive Flow will update UI automatically
                }
            }
        }
    }

    fun updateSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = timetableRepository.updateSlot(slot)) {
                is AppResult.Failure -> {
                    val msg = when (val r = result.reason) {
                        is com.studentos.core.events.AppError.DatabaseError -> r.message
                        is com.studentos.core.events.AppError.ValidationError -> r.message
                        is com.studentos.core.events.AppError.NetworkError -> r.message
                        else -> "Failed to update timetable slot"
                    }
                    _uiState.value = EditTimetableUiState.Error(msg)
                }
                is AppResult.Success -> {
                    // Reactive Flow will update UI automatically
                }
            }
        }
    }

    fun deleteSlot(slotId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = timetableRepository.deleteSlot(slotId)) {
                is AppResult.Failure -> {
                    val msg = when (val r = result.reason) {
                        is com.studentos.core.events.AppError.DatabaseError -> r.message
                        is com.studentos.core.events.AppError.ValidationError -> r.message
                        is com.studentos.core.events.AppError.NetworkError -> r.message
                        else -> "Failed to delete timetable slot"
                    }
                    _uiState.value = EditTimetableUiState.Error(msg)
                }
                is AppResult.Success -> {
                    // Reactive Flow will update UI automatically
                }
            }
        }
    }
}
