package com.studentos.feature.coding.domain.model

enum class ContestStatus {
    UPCOMING,
    ONGOING,
    COMPLETED
}

data class GroupedContests(
    val upcoming: List<CpContest> = emptyList(),
    val ongoing: List<CpContest> = emptyList(),
    val completed: List<CpContest> = emptyList()
)
