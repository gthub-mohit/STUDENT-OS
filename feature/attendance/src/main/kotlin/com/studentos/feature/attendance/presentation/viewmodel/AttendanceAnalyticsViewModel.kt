package com.studentos.feature.attendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.feature.attendance.domain.calculator.AttendanceCalculator
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import com.studentos.feature.attendance.presentation.state.AnalyticsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceAnalyticsViewModel @Inject constructor(
    private val classEventRepository: ClassEventRepository,
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch(Dispatchers.IO) {
            val thresholdStr = settingsDao.get("attendance_threshold")
            val threshold = thresholdStr?.toIntOrNull() ?: 75

            classEventRepository.getAllAttendanceSummaries().collect { summaries ->
                var overallPresent = 0
                var overallAbsent = 0
                var overallCancelled = 0
                var overallHoliday = 0
                var overallExtraPresent = 0

                for (summary in summaries) {
                    overallPresent += summary.presentCount
                    overallAbsent += summary.absentCount
                    overallCancelled += summary.cancelledCount
                    overallHoliday += summary.holidayCount
                    overallExtraPresent += summary.extraPresentCount
                }

                val overallPercentage = AttendanceCalculator.calculatePercentage(
                    present = overallPresent,
                    absent = overallAbsent,
                    cancelled = overallCancelled,
                    holiday = overallHoliday,
                    extraPresent = overallExtraPresent
                )

                val totalHeld = overallPresent + overallAbsent + overallExtraPresent
                val totalAttended = overallPresent + overallExtraPresent

                _uiState.value = AnalyticsUiState.Success(
                    summaries = summaries,
                    overallPercentage = overallPercentage,
                    totalHeldCount = totalHeld,
                    totalPresentCount = totalAttended,
                    isBelowThreshold = overallPercentage < threshold,
                    threshold = threshold
                )
            }
        }
    }
}
