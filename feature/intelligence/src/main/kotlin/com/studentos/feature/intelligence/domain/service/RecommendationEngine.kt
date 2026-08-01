package com.studentos.feature.intelligence.domain.service

import com.studentos.feature.intelligence.domain.model.RecommendationCard
import com.studentos.feature.intelligence.domain.model.fact.IntelligenceFacts
import java.util.Locale
import javax.inject.Inject

class RecommendationEngine @Inject constructor(
    private val scoringEngine: PriorityScoringEngine
) {

    fun generateRecommendations(facts: IntelligenceFacts): List<RecommendationCard> {
        val cards = mutableListOf<RecommendationCard>()

        // 1. Low Attendance Cards
        facts.attendance.lowAttendanceSubjects.forEach { subject ->
            val priority = scoringEngine.computeCardPriority("ATTENDANCE", subject.isCritical)
            val currentPctStr = String.format(Locale.US, "%.1f", subject.currentPercentage)
            val targetPctStr = String.format(Locale.US, "%.1f", subject.targetPercentage)
            cards.add(
                RecommendationCard(
                    id = "att_low_${subject.subjectId}",
                    title = "Low Attendance Warning: ${subject.subjectName}",
                    description = "Current attendance is $currentPctStr%, below target of $targetPctStr%. Attend upcoming classes to avoid shortage.",
                    category = "ATTENDANCE",
                    priority = priority,
                    actionRoute = "weekly"
                )
            )
        }

        // 2. Today's Classes Card
        if (facts.attendance.todaySlots.isNotEmpty()) {
            val count = facts.attendance.todaySlots.size
            cards.add(
                RecommendationCard(
                    id = "att_today_classes",
                    title = "Attend Today's Classes",
                    description = "You have $count scheduled class(es) today.",
                    category = "ATTENDANCE",
                    priority = scoringEngine.computeCardPriority("ATTENDANCE", false),
                    actionRoute = "weekly"
                )
            )
        }

        // 3. Overdue / Urgent Assignment Cards
        facts.assignments.urgentAssignments.forEach { assignment ->
            val isOverdue = assignment.isOverdue
            val category = "ASSIGNMENT"
            val priority = scoringEngine.computeCardPriority(category, true)
            cards.add(
                RecommendationCard(
                    id = "asgn_${assignment.id}",
                    title = if (isOverdue) "Overdue Assignment: ${assignment.title}" else "Urgent Assignment: ${assignment.title}",
                    description = if (isOverdue) "This assignment is overdue. Complete and submit as soon as possible." else "Due within 48 hours. Stay on track!",
                    category = category,
                    priority = priority,
                    actionRoute = "assignments/list"
                )
            )
        }

        // 4. Upcoming Contest Reminders
        facts.coding.upcomingContests.forEach { contest ->
            cards.add(
                RecommendationCard(
                    id = "contest_${contest.id}",
                    title = "Upcoming Contest: ${contest.name}",
                    description = "Starts in ~${contest.startsInHours} hours. Prepare your strategy and setup.",
                    category = "CONTEST",
                    priority = scoringEngine.computeCardPriority("CONTEST", contest.startsInHours <= 2),
                    actionRoute = "coding/cp"
                )
            )
        }

        // 5. Weak DSA Topic Practice
        facts.coding.suggestedDsaTopic?.let { topic ->
            cards.add(
                RecommendationCard(
                    id = "dsa_${topic.id}",
                    title = "Practice Weak DSA Topic: ${topic.name}",
                    description = "Current confidence level: ${topic.confidenceLevel}/5. Solve 1-2 problems to boost mastery.",
                    category = "DSA",
                    priority = scoringEngine.computeCardPriority("DSA", false),
                    actionRoute = "coding/knowledge-tree"
                )
            )
        }

        // 6. Free Time Suggestion (if light schedule)
        if (cards.none { it.priority == 1 } && facts.attendance.todaySlots.size <= 2) {
            cards.add(
                RecommendationCard(
                    id = "free_time_suggestion",
                    title = "Light Schedule Today",
                    description = "You have free slots today. Use this time to revise DSA or work on pending assignments.",
                    category = "FREE_TIME",
                    priority = scoringEngine.computeCardPriority("FREE_TIME", false),
                    actionRoute = "coding/knowledge-tree"
                )
            )
        }

        return cards.sortedBy { it.priority }
    }
}
