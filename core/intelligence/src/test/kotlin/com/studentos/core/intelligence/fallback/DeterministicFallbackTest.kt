package com.studentos.core.intelligence.fallback

import com.studentos.core.intelligence.snapshot.AssignmentUrgentSnapshot
import com.studentos.core.intelligence.snapshot.AttendanceWarningSnapshot
import com.studentos.core.intelligence.snapshot.ClassTodaySnapshot
import com.studentos.core.intelligence.snapshot.CpSummarySnapshot
import com.studentos.core.intelligence.snapshot.FreeSlotSnapshot
import com.studentos.core.intelligence.snapshot.IntelligenceSnapshot
import com.studentos.core.intelligence.snapshot.ScoreSnapshot
import com.studentos.core.intelligence.snapshot.StudentContextSnapshot
import com.studentos.core.intelligence.snapshot.SuggestedDsaTopicSnapshot
import com.studentos.core.intelligence.snapshot.SuggestedProjectActionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeterministicFallbackTest {

    private lateinit var fallbackEngine: DeterministicFallback

    private val fullSnapshot = IntelligenceSnapshot(
        snapshotVersion = 1,
        date = "2026-08-01",
        studentContext = StudentContextSnapshot(name = "Alex", tonePreference = "motivational"),
        classesToday = listOf(
            ClassTodaySnapshot(subject = "Algorithms", time = "09:00–10:00", location = "Hall A")
        ),
        attendanceWarnings = listOf(
            AttendanceWarningSnapshot(subject = "Maths", percentage = 60.0, threshold = 75.0, mustAttend = 3)
        ),
        assignmentsUrgent = listOf(
            AssignmentUrgentSnapshot(id = 101, subject = "Physics", title = "Overdue Lab", deadline = "1699900000000", status = "PENDING", hoursRemaining = -4),
            AssignmentUrgentSnapshot(id = 102, subject = "Algorithms", title = "Lab 1", deadline = "1700000000000", status = "PENDING", hoursRemaining = 5)
        ),
        freeSlots = listOf(
            FreeSlotSnapshot(start = "12:00", end = "13:00", durationMinutes = 60)
        ),
        suggestedDsaTopic = SuggestedDsaTopicSnapshot(category = "Graphs", topic = "BFS", confidence = 1, revisionStatus = "NOT_STARTED"),
        suggestedProjectAction = SuggestedProjectActionSnapshot(project = "Student OS", action = "Build DeterministicFallback"),
        score = ScoreSnapshot(target = 65, actual = 20),
        cpSummary = CpSummarySnapshot(codechefRating = 1800, codeforcesRating = 1600, lastSynced = "2026-08-01T08:00:00Z")
    )

    @Before
    fun setUp() {
        fallbackEngine = DeterministicFallback()
    }

    @Test
    fun generateGuidance_attendancePriority_appearsFirst() {
        val result = fallbackEngine.generateGuidance(fullSnapshot)

        assertEquals(GuidanceSource.OFFLINE, result.source)
        assertFalse(result.recommendations.isEmpty())
        assertEquals(1, result.recommendations.first().priority)
        assertEquals("ATTENDANCE", result.recommendations.first().category)
        assertTrue(result.recommendations.first().title.contains("Maths"))
    }

    @Test
    fun generateGuidance_overdueAssignments_appearsBeforeUrgent() {
        val result = fallbackEngine.generateGuidance(fullSnapshot)

        val overdueIndex = result.recommendations.indexOfFirst { it.category == "ASSIGNMENT_OVERDUE" }
        val urgentIndex = result.recommendations.indexOfFirst { it.category == "ASSIGNMENT_URGENT" }

        assertTrue(overdueIndex != -1)
        assertTrue(urgentIndex != -1)
        assertTrue("Overdue assignments (priority 2) must appear before urgent (priority 3)", overdueIndex < urgentIndex)
    }

    @Test
    fun generateGuidance_contestReminder_included() {
        val result = fallbackEngine.generateGuidance(fullSnapshot)

        val contestItem = result.recommendations.find { it.category == "CONTEST" }
        assertTrue(contestItem != null)
        assertEquals(5, contestItem?.priority)
        assertTrue(contestItem?.description?.contains("1800") == true)
    }

    @Test
    fun generateGuidance_dsaRecommendation_included() {
        val result = fallbackEngine.generateGuidance(fullSnapshot)

        val dsaItem = result.recommendations.find { it.category == "DSA" }
        assertTrue(dsaItem != null)
        assertEquals(6, dsaItem?.priority)
        assertTrue(dsaItem?.title?.contains("BFS") == true)
    }

    @Test
    fun generateGuidance_projectRecommendation_included() {
        val result = fallbackEngine.generateGuidance(fullSnapshot)

        val projectItem = result.recommendations.find { it.category == "PROJECT" }
        assertTrue(projectItem != null)
        assertEquals(7, projectItem?.priority)
        assertTrue(projectItem?.description?.contains("Build DeterministicFallback") == true)
    }

    @Test
    fun generateGuidance_emptySnapshot_returnsHealthySummary() {
        val emptySnapshot = IntelligenceSnapshot(
            snapshotVersion = 1,
            date = "2026-08-01",
            studentContext = StudentContextSnapshot(name = null, tonePreference = "motivational"),
            classesToday = emptyList(),
            attendanceWarnings = emptyList(),
            assignmentsUrgent = emptyList(),
            freeSlots = emptyList(),
            suggestedDsaTopic = null,
            suggestedProjectAction = null,
            score = ScoreSnapshot(target = 0, actual = 0),
            cpSummary = CpSummarySnapshot(codechefRating = 0, codeforcesRating = 0)
        )

        val result = fallbackEngine.generateGuidance(emptySnapshot)

        assertTrue(result.recommendations.isEmpty())
        assertTrue(result.summary.contains("All tasks completed"))
        assertEquals(GuidanceSource.OFFLINE, result.source)
    }

    @Test
    fun generateGuidance_deterministicOutput_forIdenticalInputs() {
        val result1 = fallbackEngine.generateGuidance(fullSnapshot)
        val result2 = fallbackEngine.generateGuidance(fullSnapshot)

        assertEquals(result1, result2)
    }

    @Test
    fun generateGuidance_recommendationOrdering_strictlyFollowsPriorityHierarchy() {
        val result = fallbackEngine.generateGuidance(fullSnapshot)

        val priorities = result.recommendations.map { it.priority }
        val sortedPriorities = priorities.sorted()

        assertEquals("Recommendations must be sorted by priority ASC strictly", sortedPriorities, priorities)
    }
}
