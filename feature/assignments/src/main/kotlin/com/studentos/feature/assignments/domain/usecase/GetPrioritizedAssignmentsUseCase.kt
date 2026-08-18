package com.studentos.feature.assignments.domain.usecase

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.assignments.domain.model.PrioritizedAssignmentGroup
import com.studentos.feature.assignments.domain.model.UrgencyCategory
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * GetPrioritizedAssignmentsUseCase — Domain use case for categorizing and prioritizing assignments by deadline urgency.
 */
class GetPrioritizedAssignmentsUseCase @Inject constructor(
    private val repository: AssignmentRepository
) {
    operator fun invoke(
        nowEpoch: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<List<PrioritizedAssignmentGroup>> {
        return repository.getAllAssignments().map { assignments ->
            categorizeAndPrioritize(assignments, nowEpoch, zoneId)
        }
    }

    fun categorizeAndPrioritize(
        assignments: List<AssignmentEntity>,
        nowEpoch: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<PrioritizedAssignmentGroup> {
        val today = Instant.ofEpochMilli(nowEpoch).atZone(zoneId).toLocalDate()
        val tomorrow = today.plusDays(1)
        val endOfWeek = today.plusDays(7)

        val grouped = mutableMapOf<UrgencyCategory, MutableList<AssignmentEntity>>()
        UrgencyCategory.entries.forEach { grouped[it] = mutableListOf() }

        for (assignment in assignments) {
            val isFinished = assignment.status == AssignmentEntity.STATUS_SUBMITTED ||
                    assignment.status == AssignmentEntity.STATUS_COMPLETED

            val deadlineDate = Instant.ofEpochMilli(assignment.deadline).atZone(zoneId).toLocalDate()

            val category = when {
                !isFinished && assignment.deadline < nowEpoch -> UrgencyCategory.OVERDUE
                deadlineDate.isEqual(today) -> UrgencyCategory.DUE_TODAY
                deadlineDate.isEqual(tomorrow) -> UrgencyCategory.DUE_TOMORROW
                !deadlineDate.isBefore(today) && !deadlineDate.isAfter(endOfWeek) -> UrgencyCategory.DUE_THIS_WEEK
                else -> UrgencyCategory.LATER
            }

            grouped[category]?.add(assignment)
        }

        return UrgencyCategory.entries.mapNotNull { cat ->
            val list = grouped[cat]
            if (!list.isNullOrEmpty()) {
                val sortedList = list.sortedWith(
                    compareBy<AssignmentEntity> { it.deadline }
                        .thenBy { getPriorityWeight(it.priority) }
                )
                PrioritizedAssignmentGroup(category = cat, assignments = sortedList)
            } else {
                null
            }
        }
    }

    private fun getPriorityWeight(priority: String): Int {
        return when (priority.uppercase()) {
            AssignmentEntity.PRIORITY_HIGH -> 1
            AssignmentEntity.PRIORITY_MEDIUM -> 2
            AssignmentEntity.PRIORITY_LOW -> 3
            else -> 4
        }
    }
}
