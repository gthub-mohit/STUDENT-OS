package com.studentos.feature.attendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.calculator.AttendanceCalculator
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import com.studentos.feature.attendance.domain.usecase.AddExtraClassUseCase
import com.studentos.feature.attendance.domain.usecase.UpdateClassEventStatusUseCase
import com.studentos.feature.attendance.presentation.state.WeeklyUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class WeeklyViewModel @Inject constructor(
    private val classEventRepository: ClassEventRepository,
    private val subjectRepository: SubjectRepository,
    private val settingsDao: SettingsDao,
    private val updateClassEventStatusUseCase: UpdateClassEventStatusUseCase,
    private val addExtraClassUseCase: AddExtraClassUseCase
) : ViewModel() {

    private val _selectedDayOfWeek = MutableStateFlow(currentDayOfWeek())
    private val _uiState = MutableStateFlow<WeeklyUiState>(WeeklyUiState.Loading)
    val uiState: StateFlow<WeeklyUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            val thresholdStr = settingsDao.get("attendance_threshold")
            val threshold = thresholdStr?.toIntOrNull() ?: 75

            val (startWeekMs, endWeekMs) = currentWeekBounds()

            combine(
                _selectedDayOfWeek,
                classEventRepository.getEventsForWeek(startWeekMs, endWeekMs),
                subjectRepository.getActiveSubjects()
            ) { selectedDay, weekEvents, activeSubjects ->
                val dayEvents = weekEvents.filter { event ->
                    getEventDayOfWeek(event.scheduledAt) == selectedDay
                }

                var totalPresent = 0
                var totalAbsent = 0
                var totalCancelled = 0
                var totalHoliday = 0
                var totalExtraPresent = 0

                for (event in weekEvents) {
                    when (event.status) {
                        ClassEventEntity.STATUS_PRESENT -> totalPresent++
                        ClassEventEntity.STATUS_ABSENT -> totalAbsent++
                        ClassEventEntity.STATUS_CANCELLED -> totalCancelled++
                        ClassEventEntity.STATUS_HOLIDAY -> totalHoliday++
                        ClassEventEntity.STATUS_EXTRA_CLASS -> totalExtraPresent++
                    }
                }

                val overallPct = AttendanceCalculator.calculatePercentage(
                    present = totalPresent,
                    absent = totalAbsent,
                    cancelled = totalCancelled,
                    holiday = totalHoliday,
                    extraPresent = totalExtraPresent
                )

                WeeklyUiState.Success(
                    selectedDayOfWeek = selectedDay,
                    dayEvents = dayEvents,
                    subjects = activeSubjects,
                    overallAttendancePercentage = overallPct,
                    isBelowThreshold = overallPct < threshold,
                    threshold = threshold
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectDay(dayOfWeek: Int) {
        _selectedDayOfWeek.value = dayOfWeek
    }

    fun updateEventStatus(eventId: Long, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            updateClassEventStatusUseCase(eventId, status)
        }
    }

    fun addExtraClass(
        subjectId: Long,
        scheduledAt: Long,
        endAt: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            addExtraClassUseCase(subjectId, scheduledAt, endAt)
        }
    }

    private fun currentDayOfWeek(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    private fun currentWeekBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 7)
        val endMs = cal.timeInMillis - 1
        return Pair(startMs, endMs)
    }

    private fun getEventDayOfWeek(epochMs: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }
}
