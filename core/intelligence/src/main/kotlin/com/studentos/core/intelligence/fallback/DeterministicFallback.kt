package com.studentos.core.intelligence.fallback

import com.studentos.core.intelligence.snapshot.IntelligenceSnapshot
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeterministicFallback @Inject constructor() {

    fun generateGuidance(snapshot: IntelligenceSnapshot): GuidanceResult {
        val items = mutableListOf<GuidanceItem>()

        // 1. Critical Attendance (Priority 1)
        for (warn in snapshot.attendanceWarnings) {
            val desc = if (warn.mustAttend > 0) {
                "Current attendance is ${warn.percentage}%. Must attend at least ${warn.mustAttend} upcoming classes to reach ${warn.threshold}%."
            } else {
                "Current attendance is ${warn.percentage}%. Target threshold is ${warn.threshold}%."
            }
            items.add(
                GuidanceItem(
                    priority = 1,
                    category = "ATTENDANCE",
                    title = "Attendance Warning: ${warn.subject}",
                    description = desc
                )
            )
        }

        // 2. Overdue Assignments (Priority 2)
        val overdueAssignments = snapshot.assignmentsUrgent.filter { it.hoursRemaining < 0 }
        for (assign in overdueAssignments) {
            val overdueHours = Math.abs(assign.hoursRemaining)
            items.add(
                GuidanceItem(
                    priority = 2,
                    category = "ASSIGNMENT_OVERDUE",
                    title = "Overdue Assignment: ${assign.title}",
                    description = "${assign.subject} assignment was due ${overdueHours}h ago. Complete and submit immediately."
                )
            )
        }

        // 3. Assignments Due Today / Soon (Priority 3)
        val dueSoonAssignments = snapshot.assignmentsUrgent.filter { it.hoursRemaining in 0..24 }
        for (assign in dueSoonAssignments) {
            items.add(
                GuidanceItem(
                    priority = 3,
                    category = "ASSIGNMENT_URGENT",
                    title = "Assignment Due Soon: ${assign.title}",
                    description = "${assign.subject} assignment is due in ${assign.hoursRemaining}h (${assign.status})."
                )
            )
        }

        // 4. Upcoming Classes Today (Priority 4)
        for (cls in snapshot.classesToday) {
            val locSuffix = if (!cls.location.isNullOrEmpty()) " in ${cls.location}" else ""
            items.add(
                GuidanceItem(
                    priority = 4,
                    category = "CLASS",
                    title = "Class Today: ${cls.subject}",
                    description = "Scheduled at ${cls.time}${locSuffix}."
                )
            )
        }

        // 5. Competitive Programming / Contest (Priority 5)
        if (snapshot.cpSummary.codechefRating > 0 || snapshot.cpSummary.codeforcesRating > 0) {
            items.add(
                GuidanceItem(
                    priority = 5,
                    category = "CONTEST",
                    title = "Competitive Programming Focus",
                    description = "Current Ratings — CodeChef: ${snapshot.cpSummary.codechefRating}, Codeforces: ${snapshot.cpSummary.codeforcesRating}. Practice a contest set today."
                )
            )
        }

        // 6. Weak DSA Topic (Priority 6)
        snapshot.suggestedDsaTopic?.let { dsa ->
            items.add(
                GuidanceItem(
                    priority = 6,
                    category = "DSA",
                    title = "Revise DSA: ${dsa.topic}",
                    description = "Category: ${dsa.category}. Confidence level: ${dsa.confidence}/5 (Status: ${dsa.revisionStatus})."
                )
            )
        }

        // 7. Project Next Action (Priority 7)
        snapshot.suggestedProjectAction?.let { proj ->
            items.add(
                GuidanceItem(
                    priority = 7,
                    category = "PROJECT",
                    title = "Project Action: ${proj.project}",
                    description = "Next Action: ${proj.action}."
                )
            )
        }

        // 8. Free Slot Utilization (Priority 8)
        for (slot in snapshot.freeSlots) {
            items.add(
                GuidanceItem(
                    priority = 8,
                    category = "FREE_SLOT",
                    title = "Free Slot: ${slot.start}–${slot.end}",
                    description = "${slot.durationMinutes} minutes available. Use this time for DSA revision or project tasks."
                )
            )
        }

        // Sort items by priority ASC strictly
        val sortedItems = items.sortedBy { it.priority }

        val summaryText = if (sortedItems.isNotEmpty()) {
            "Top focus for today is ${sortedItems.first().title}."
        } else {
            "All tasks completed and attendance is healthy. Great job!"
        }

        return GuidanceResult(
            summary = summaryText,
            recommendations = sortedItems,
            source = GuidanceSource.OFFLINE
        )
    }
}
