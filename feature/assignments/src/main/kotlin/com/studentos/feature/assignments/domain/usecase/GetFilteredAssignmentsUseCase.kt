package com.studentos.feature.assignments.domain.usecase

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.assignments.domain.model.AssignmentFilter
import com.studentos.feature.assignments.domain.model.TaskType
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * GetFilteredAssignmentsUseCase — Domain use case for retrieving assignments filtered by status/timeline and task type.
 */
class GetFilteredAssignmentsUseCase @Inject constructor(
    private val repository: AssignmentRepository
) {
    operator fun invoke(
        filter: AssignmentFilter = AssignmentFilter.ALL,
        taskType: TaskType? = null,
        statusFilter: String? = null,
        deadlineFilter: AssignmentFilter? = null,
        nowEpoch: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Flow<List<AssignmentEntity>> {
        if (statusFilter != null || (deadlineFilter != null && deadlineFilter != AssignmentFilter.ALL)) {
            return repository.getAllAssignments().map { list ->
                list.filter { entity ->
                    val matchesType = taskType == null || TaskType.fromString(entity.taskType) == taskType
                    val matchesStatus = statusFilter == null || entity.status.equals(statusFilter, ignoreCase = true)
                    val matchesDeadline = if (deadlineFilter == null || deadlineFilter == AssignmentFilter.ALL) {
                        true
                    } else {
                        val zonedDateTime = Instant.ofEpochMilli(nowEpoch).atZone(zoneId)
                        val startOfDay = zonedDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                        val endOfDay = zonedDateTime.toLocalDate().atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
                        val endOfWeek = zonedDateTime.toLocalDate().plusDays(6).atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
                        when (deadlineFilter) {
                            AssignmentFilter.TODAY -> entity.deadline in startOfDay..endOfDay
                            AssignmentFilter.THIS_WEEK -> entity.deadline in startOfDay..endOfWeek
                            AssignmentFilter.OVERDUE -> entity.deadline < nowEpoch && entity.status != AssignmentEntity.STATUS_COMPLETED && entity.status != AssignmentEntity.STATUS_SUBMITTED
                            else -> true
                        }
                    }
                    matchesType && matchesStatus && matchesDeadline
                }.sortedBy { it.deadline }
            }
        }

        val baseFlow = when (filter) {
            AssignmentFilter.ALL -> {
                repository.getAllAssignments()
            }
            AssignmentFilter.PENDING -> {
                repository.getAssignmentsByStatus(AssignmentEntity.STATUS_PENDING)
            }
            AssignmentFilter.IN_PROGRESS -> {
                repository.getAssignmentsByStatus(AssignmentEntity.STATUS_IN_PROGRESS)
            }
            AssignmentFilter.TODAY -> {
                val zonedDateTime = Instant.ofEpochMilli(nowEpoch).atZone(zoneId)
                val startOfDay = zonedDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endOfDay = zonedDateTime.toLocalDate().atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
                repository.getAssignmentsToday(startOfDay, endOfDay)
            }
            AssignmentFilter.THIS_WEEK -> {
                val zonedDateTime = Instant.ofEpochMilli(nowEpoch).atZone(zoneId)
                val startOfDay = zonedDateTime.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endOfWeek = zonedDateTime.toLocalDate().plusDays(6).atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
                repository.getAssignmentsThisWeek(startOfDay, endOfWeek)
            }
            AssignmentFilter.OVERDUE -> {
                repository.getOverdueAssignments(nowEpoch)
            }
            AssignmentFilter.COMPLETED -> {
                repository.getAssignmentsByStatus(AssignmentEntity.STATUS_COMPLETED)
            }
        }

        return if (taskType == null) {
            baseFlow
        } else {
            baseFlow.map { list ->
                list.filter { TaskType.fromString(it.taskType) == taskType }
            }
        }
    }
}
