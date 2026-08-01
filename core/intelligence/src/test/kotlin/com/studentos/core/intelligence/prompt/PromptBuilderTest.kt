package com.studentos.core.intelligence.prompt

import com.studentos.core.intelligence.snapshot.AssignmentUrgentSnapshot
import com.studentos.core.intelligence.snapshot.AttendanceWarningSnapshot
import com.studentos.core.intelligence.snapshot.ClassTodaySnapshot
import com.studentos.core.intelligence.snapshot.CpSummarySnapshot
import com.studentos.core.intelligence.snapshot.FreeSlotSnapshot
import com.studentos.core.intelligence.snapshot.IntelligenceSnapshot
import com.studentos.core.intelligence.snapshot.ScoreSnapshot
import com.studentos.core.intelligence.snapshot.SnapshotDiffer
import com.studentos.core.intelligence.snapshot.StudentContextSnapshot
import com.studentos.core.intelligence.snapshot.SuggestedDsaTopicSnapshot
import com.studentos.core.intelligence.snapshot.SuggestedProjectActionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PromptBuilderTest {

    private lateinit var promptBuilder: PromptBuilder
    private lateinit var snapshotDiffer: SnapshotDiffer

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
            AssignmentUrgentSnapshot(id = 101, subject = "Algorithms", title = "Lab 1", deadline = "1700000000000", status = "PENDING", hoursRemaining = 5)
        ),
        freeSlots = listOf(
            FreeSlotSnapshot(start = "12:00", end = "13:00", durationMinutes = 60)
        ),
        suggestedDsaTopic = SuggestedDsaTopicSnapshot(category = "Graphs", topic = "BFS", confidence = 1, revisionStatus = "NOT_STARTED"),
        suggestedProjectAction = SuggestedProjectActionSnapshot(project = "Student OS", action = "Build PromptBuilder"),
        score = ScoreSnapshot(target = 65, actual = 20),
        cpSummary = CpSummarySnapshot(codechefRating = 1800, codeforcesRating = 1600, lastSynced = "2026-08-01T08:00:00Z")
    )

    @Before
    fun setUp() {
        promptBuilder = PromptBuilder()
        snapshotDiffer = SnapshotDiffer()
    }

    @Test
    fun buildMorningPrompt_fullSnapshot_formatsCompactPromptWithinBudget() {
        val prompt = promptBuilder.buildMorningPrompt(fullSnapshot)

        assertTrue(prompt.contains("DATE=2026-08-01"))
        assertTrue(prompt.contains("CONTEXT: Name=Alex, Tone=motivational"))
        assertTrue(prompt.contains("CLASSES:\n- 09:00–10:00 Algorithms @ Hall A"))
        assertTrue(prompt.contains("ATT_WARN:\n- Maths 60.0% (Must attend: 3)"))
        assertTrue(prompt.contains("URGENT_ASSIGNMENTS:\n- [101] Algorithms: Lab 1 (Status: PENDING, Due: 5h)"))
        assertTrue(prompt.contains("FREE_SLOTS:\n- 12:00-13:00 (60m)"))
        assertTrue(prompt.contains("DSA:\n- Graphs -> BFS (Conf: 1, Status: NOT_STARTED)"))
        assertTrue(prompt.contains("PROJECT:\n- Student OS -> Build PromptBuilder"))
        assertTrue(prompt.contains("CP:\n- CC=1800, CF=1600"))
        assertTrue(prompt.contains("SCORE:\nTarget=65, Actual=20"))

        // Estimate token budget (1 token ~ 4 chars). Prompt length must easily stay under 1600 chars (400 tokens)
        assertTrue("Morning prompt should be compact (< 1600 chars)", prompt.length < 1600)
    }

    @Test
    fun buildMorningPrompt_emptySnapshot_omitsEmptySections() {
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

        val prompt = promptBuilder.buildMorningPrompt(emptySnapshot)

        assertTrue(prompt.contains("DATE=2026-08-01"))
        assertFalse(prompt.contains("CLASSES:"))
        assertFalse(prompt.contains("ATT_WARN:"))
        assertFalse(prompt.contains("URGENT_ASSIGNMENTS:"))
        assertFalse(prompt.contains("FREE_SLOTS:"))
        assertFalse(prompt.contains("DSA:"))
        assertFalse(prompt.contains("PROJECT:"))
        assertFalse(prompt.contains("CP:"))
        assertTrue(prompt.contains("SCORE:\nTarget=0, Actual=0"))
    }

    @Test
    fun buildDeltaPrompt_emptyDelta_returnsNoChanges() {
        val delta = snapshotDiffer.diff(fullSnapshot, fullSnapshot)
        val prompt = promptBuilder.buildDeltaPrompt(fullSnapshot, fullSnapshot, delta)

        assertEquals("DELTA: NO_CHANGES", prompt)
    }

    @Test
    fun buildDeltaPrompt_attendanceOnly_omitsUnchangedSections() {
        val updatedWarning = fullSnapshot.attendanceWarnings.first().copy(percentage = 55.0, mustAttend = 4)
        val updatedSnapshot = fullSnapshot.copy(attendanceWarnings = listOf(updatedWarning))
        val delta = snapshotDiffer.diff(fullSnapshot, updatedSnapshot)

        val prompt = promptBuilder.buildDeltaPrompt(fullSnapshot, updatedSnapshot, delta)

        assertTrue(prompt.contains("DELTA [2026-08-01]"))
        assertTrue(prompt.contains("ATTENDANCE_CHANGED:"))
        assertTrue(prompt.contains("~ Updated: Maths 55.0%"))
        assertFalse(prompt.contains("CLASSES_CHANGED:"))
        assertFalse(prompt.contains("ASSIGNMENTS_CHANGED:"))
        assertFalse(prompt.contains("FREE_SLOTS_CHANGED:"))
        assertFalse(prompt.contains("DSA_CHANGED:"))
        assertFalse(prompt.contains("PROJECT_ACTION_CHANGED:"))
        assertFalse(prompt.contains("CP_CHANGED:"))

        // Delta prompt must stay under 600 chars (150 tokens)
        assertTrue("Delta prompt should be compact (< 600 chars)", prompt.length < 600)
    }

    @Test
    fun buildDeltaPrompt_assignmentsOnly_omitsUnchangedSections() {
        val newAssignment = AssignmentUrgentSnapshot(id = 102, subject = "Physics", title = "Lab 2", deadline = "1700000000000", status = "PENDING", hoursRemaining = 12)
        val updatedSnapshot = fullSnapshot.copy(assignmentsUrgent = listOf(fullSnapshot.assignmentsUrgent.first(), newAssignment))
        val delta = snapshotDiffer.diff(fullSnapshot, updatedSnapshot)

        val prompt = promptBuilder.buildDeltaPrompt(fullSnapshot, updatedSnapshot, delta)

        assertTrue(prompt.contains("ASSIGNMENTS_CHANGED:"))
        assertTrue(prompt.contains("+ Added: Physics: Lab 2 (Due: 12h)"))
        assertFalse(prompt.contains("ATTENDANCE_CHANGED:"))
        assertFalse(prompt.contains("CP_CHANGED:"))
    }

    @Test
    fun buildMorningPrompt_deterministicOutput() {
        val prompt1 = promptBuilder.buildMorningPrompt(fullSnapshot)
        val prompt2 = promptBuilder.buildMorningPrompt(fullSnapshot)

        assertEquals(prompt1, prompt2)
    }

    @Test
    fun buildDeltaPrompt_deterministicOutput() {
        val delta = snapshotDiffer.diff(fullSnapshot, fullSnapshot)
        val prompt1 = promptBuilder.buildDeltaPrompt(fullSnapshot, fullSnapshot, delta)
        val prompt2 = promptBuilder.buildDeltaPrompt(fullSnapshot, fullSnapshot, delta)

        assertEquals(prompt1, prompt2)
    }
}
