package com.studentos.feature.attendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import com.studentos.feature.attendance.presentation.state.CalendarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val classEventRepository: ClassEventRepository
) : ViewModel() {

    private val _currentMonthMs = MutableStateFlow(startOfMonthMs(0))
    private val _selectedDateMs = MutableStateFlow(System.currentTimeMillis())
    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                _currentMonthMs,
                _selectedDateMs
            ) { monthMs, selectedMs ->
                monthMs to selectedMs
            }.flatMapLatest { (monthMs, selectedMs) ->
                val (startMs, endMs) = getMonthBounds(monthMs)
                classEventRepository.getEventsForWeek(startMs, endMs).map { monthEvents ->
                    val (dayStart, dayEnd) = getDayBounds(selectedMs)
                    val selectedDayEvents = monthEvents.filter { event ->
                        event.scheduledAt in dayStart..dayEnd
                    }

                    CalendarUiState.Success(
                        currentMonthEpochMs = monthMs,
                        selectedDateEpochMs = selectedMs,
                        monthEvents = monthEvents,
                        selectedDayEvents = selectedDayEvents
                    )
                }
            }.catch { error ->
                _uiState.value = CalendarUiState.Error(error.message ?: "Failed to load calendar")
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectDate(epochMs: Long) {
        _selectedDateMs.value = epochMs
    }

    fun changeMonth(monthOffset: Int) {
        _currentMonthMs.value = startOfMonthMs(monthOffset)
    }

    private fun startOfMonthMs(offset: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getMonthBounds(monthMs: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = monthMs
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startMs = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        val endMs = cal.timeInMillis - 1
        return Pair(startMs, endMs)
    }

    private fun getDayBounds(dateMs: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endMs = cal.timeInMillis - 1
        return Pair(startMs, endMs)
    }
}
