package com.studentos.feature.intelligence.domain.analyzer

import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.intelligence.domain.model.fact.AssignmentFact
import com.studentos.feature.intelligence.domain.model.fact.AssignmentItemFact
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

class AssignmentAnalyzer @Inject constructor(
    private val assignmentDao: AssignmentDao,
    private val clock: Clock
) : IntelligenceAnalyzer {

    override val key: String = KEY

    override suspend fun analyze(todayDate: String): AssignmentFact {
        val nowMs = clock.millis()
        val pendingAssignments: List<AssignmentEntity> = assignmentDao.getAssignmentsByStatus("PENDING").first()

        val itemFacts = pendingAssignments.map { assignment ->
            val isOverdue = assignment.deadline < nowMs
            val isUrgent = isOverdue || ((assignment.deadline - nowMs) <= 48 * 3600 * 1000L)
            AssignmentItemFact(
                id = assignment.id,
                title = assignment.title,
                deadlineEpochMs = assignment.deadline,
                isOverdue = isOverdue,
                isUrgent = isUrgent
            )
        }

        val overdueCount = itemFacts.count { it.isOverdue }
        val urgentList = itemFacts.filter { it.isUrgent }

        return AssignmentFact(
            overdueCount = overdueCount,
            urgentAssignments = urgentList,
            totalPendingCount = itemFacts.size
        )
    }

    companion object {
        const val KEY = "assignment"
    }
}
