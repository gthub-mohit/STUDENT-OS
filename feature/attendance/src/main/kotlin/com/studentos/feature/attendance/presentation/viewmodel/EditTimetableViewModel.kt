package com.studentos.feature.attendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import com.studentos.feature.attendance.presentation.state.EditTimetableUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTimetableViewModel @Inject constructor(
    private val timetableSlotDao: TimetableSlotDao,
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
                timetableSlotDao.getAllSlots(),
                subjectRepository.getActiveSubjects(),
                _selectedDay
            ) { slots, subjects, selectedDay ->
                EditTimetableUiState.Success(
                    slots = slots,
                    subjects = subjects,
                    selectedDayOfWeek = selectedDay
                )
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
            try {
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
                timetableSlotDao.insert(newSlot)
            } catch (e: Exception) {
                _uiState.value = EditTimetableUiState.Error(e.message ?: "Failed to add timetable slot")
            }
        }
    }

    fun updateSlot(slot: TimetableSlotEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                timetableSlotDao.update(slot)
            } catch (e: Exception) {
                _uiState.value = EditTimetableUiState.Error(e.message ?: "Failed to update timetable slot")
            }
        }
    }

    fun deleteSlot(slotId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                timetableSlotDao.deleteById(slotId)
            } catch (e: Exception) {
                _uiState.value = EditTimetableUiState.Error(e.message ?: "Failed to delete timetable slot")
            }
        }
    }
}
