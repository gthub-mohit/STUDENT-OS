package com.studentos.feature.coding.presentation.state

import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.CpProfile

data class CpDashboardUiState(
    val isLoading: Boolean = true,
    val profiles: List<CpProfile> = emptyList(),
    val contests: List<CpContest> = emptyList(),
    val lastSyncedAt: Long? = null,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && profiles.isEmpty()
}
