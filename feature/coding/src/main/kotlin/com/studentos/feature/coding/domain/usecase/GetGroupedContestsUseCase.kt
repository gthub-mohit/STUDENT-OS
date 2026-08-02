package com.studentos.feature.coding.domain.usecase

import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.GroupedContests
import com.studentos.feature.coding.domain.repository.CpRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * GetGroupedContestsUseCase — Domain use case for grouping contests into Upcoming, Ongoing, and Completed categories.
 */
class GetGroupedContestsUseCase @Inject constructor(
    private val repository: CpRepository
) {
    operator fun invoke(nowEpochMs: Long = System.currentTimeMillis()): Flow<GroupedContests> {
        return repository.getAllContests().map { contests ->
            groupContests(contests, nowEpochMs)
        }
    }

    fun groupContests(contests: List<CpContest>, nowEpochMs: Long = System.currentTimeMillis()): GroupedContests {
        val upcoming = mutableListOf<CpContest>()
        val ongoing = mutableListOf<CpContest>()
        val completed = mutableListOf<CpContest>()

        val ongoingThresholdMs = 2 * 3600 * 1000L // 2 hours

        for (contest in contests) {
            val date = contest.contestDate
            when {
                date > nowEpochMs + 900000L -> upcoming.add(contest)
                date in (nowEpochMs - ongoingThresholdMs)..(nowEpochMs + 900000L) -> ongoing.add(contest)
                else -> completed.add(contest)
            }
        }

        return GroupedContests(
            upcoming = upcoming.sortedBy { it.contestDate },
            ongoing = ongoing.sortedBy { it.contestDate },
            completed = completed.sortedByDescending { it.contestDate }
        )
    }
}
