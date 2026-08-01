package com.studentos.feature.intelligence.presentation.state

import com.studentos.core.intelligence.snapshot.AssignmentUrgentSnapshot
import com.studentos.core.intelligence.snapshot.AttendanceWarningSnapshot
import com.studentos.core.intelligence.snapshot.FreeSlotSnapshot
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.model.RecommendationCard

data class DailyBriefUiState(
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val dailyBrief: DailyBrief? = null,
    val recommendations: List<RecommendationCard> = emptyList(),
    val attendanceWarnings: List<AttendanceWarningSnapshot> = emptyList(),
    val urgentAssignments: List<AssignmentUrgentSnapshot> = emptyList(),
    val freeSlots: List<FreeSlotSnapshot> = emptyList(),
    val errorMessage: String? = null,
    val isEmpty: Boolean = false,
    val todayDate: String = "",
    val lastUpdatedFormatted: String = ""
)
