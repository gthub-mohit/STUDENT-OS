package com.studentos.feature.intelligence.domain.service

import com.studentos.feature.intelligence.domain.model.fact.IntelligenceFacts
import javax.inject.Inject

class PriorityScoringEngine @Inject constructor() {

    fun calculateTargetScore(facts: IntelligenceFacts): Int {
        var baseScore = 100

        val criticalAttendanceDeduction = facts.attendance.lowAttendanceSubjects.count { it.isCritical } * 15
        val overdueAssignmentDeduction = facts.assignments.overdueCount * 20
        val urgentAssignmentDeduction = (facts.assignments.urgentAssignments.size - facts.assignments.overdueCount) * 10

        val totalDeduction = criticalAttendanceDeduction + overdueAssignmentDeduction + urgentAssignmentDeduction
        return maxOf(20, baseScore - totalDeduction)
    }

    fun computeCardPriority(category: String, isUrgentOrCritical: Boolean): Int {
        return when (category) {
            "ATTENDANCE" -> if (isUrgentOrCritical) 1 else 3
            "ASSIGNMENT" -> if (isUrgentOrCritical) 1 else 2
            "CONTEST" -> if (isUrgentOrCritical) 2 else 4
            "DSA" -> 3
            "FREE_TIME" -> 5
            else -> 4
        }
    }
}
