package com.studentos.core.intelligence.snapshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SnapshotDifferTest {

    private lateinit var snapshotDiffer: SnapshotDiffer

    private val baseSnapshot = IntelligenceSnapshot(
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
        suggestedProjectAction = SuggestedProjectActionSnapshot(project = "Student OS", action = "Implement SnapshotDiffer"),
        score = ScoreSnapshot(target = 65, actual = 20),
        cpSummary = CpSummarySnapshot(codechefRating = 1800, codeforcesRating = 1600, lastSynced = "2026-08-01T08:00:00Z")
    )

    @Before
    fun setUp() {
        snapshotDiffer = SnapshotDiffer()
    }

    @Test
    fun diff_identicalSnapshots_returnsEmptyDelta() {
        val delta = snapshotDiffer.diff(baseSnapshot, baseSnapshot)
        assertTrue(delta.isEmpty)
        assertFalse(delta.dateChanged)
        assertFalse(delta.studentContextChanged)
    }

    @Test
    fun diff_nullOldSnapshot_returnsFullDelta() {
        val delta = snapshotDiffer.diff(null, baseSnapshot)
        assertFalse(delta.isEmpty)
        assertTrue(delta.dateChanged)
        assertTrue(delta.studentContextChanged)
        assertEquals(1, delta.classesDelta.added.size)
        assertEquals(1, delta.attendanceDelta.addedWarnings.size)
        assertEquals(1, delta.assignmentsDelta.added.size)
        assertEquals(1, delta.freeSlotsDelta.added.size)
        assertTrue(delta.dsaTopicDelta.changed)
        assertTrue(delta.projectActionDelta.changed)
        assertTrue(delta.scoreDelta.targetChanged)
        assertTrue(delta.cpSummaryDelta.ratingChanged)
    }

    @Test
    fun diff_timestampOnlyChange_ignored() {
        // Last synced timestamp changed, but ratings remain identical
        val newSnapshot = baseSnapshot.copy(
            cpSummary = baseSnapshot.cpSummary.copy(lastSynced = "2026-08-01T12:00:00Z")
        )

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertTrue("Timestamp-only change must be ignored", delta.isEmpty)
    }

    @Test
    fun diff_attendanceChange_detected() {
        val updatedWarning = baseSnapshot.attendanceWarnings.first().copy(percentage = 55.0, mustAttend = 4)
        val newSnapshot = baseSnapshot.copy(attendanceWarnings = listOf(updatedWarning))

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertFalse(delta.isEmpty)
        assertEquals(1, delta.attendanceDelta.updatedWarnings.size)
        assertEquals(55.0, delta.attendanceDelta.updatedWarnings.first().percentage, 0.01)
    }

    @Test
    fun diff_attendanceWarningAddedAndRemoved_detected() {
        val newWarning = AttendanceWarningSnapshot(subject = "Physics", percentage = 70.0, threshold = 75.0, mustAttend = 1)
        val newSnapshot = baseSnapshot.copy(attendanceWarnings = listOf(newWarning))

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertFalse(delta.isEmpty)
        assertEquals(1, delta.attendanceDelta.addedWarnings.size)
        assertEquals("Physics", delta.attendanceDelta.addedWarnings.first().subject)
        assertEquals(1, delta.attendanceDelta.removedWarnings.size)
        assertEquals("Maths", delta.attendanceDelta.removedWarnings.first().subject)
    }

    @Test
    fun diff_assignmentAdditionAndRemoval_detected() {
        val newAssignment = AssignmentUrgentSnapshot(id = 102, subject = "Physics", title = "Lab 2", deadline = "1700000000000", status = "PENDING", hoursRemaining = 12)
        val newSnapshot = baseSnapshot.copy(assignmentsUrgent = listOf(newAssignment))

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertFalse(delta.isEmpty)
        assertEquals(1, delta.assignmentsDelta.added.size)
        assertEquals(102L, delta.assignmentsDelta.added.first().id)
        assertEquals(1, delta.assignmentsDelta.removed.size)
        assertEquals(101L, delta.assignmentsDelta.removed.first().id)
    }

    @Test
    fun diff_urgentAssignmentHoursRemainingChange_detected() {
        val updatedAssignment = baseSnapshot.assignmentsUrgent.first().copy(hoursRemaining = 2)
        val newSnapshot = baseSnapshot.copy(assignmentsUrgent = listOf(updatedAssignment))

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertFalse(delta.isEmpty)
        assertEquals(1, delta.assignmentsDelta.updated.size)
        assertEquals(2L, delta.assignmentsDelta.updated.first().hoursRemaining)
    }

    @Test
    fun diff_freeSlotChanges_detected() {
        val newSnapshot = baseSnapshot.copy(freeSlots = emptyList())

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertFalse(delta.isEmpty)
        assertEquals(1, delta.freeSlotsDelta.removed.size)
        assertEquals("12:00", delta.freeSlotsDelta.removed.first().start)
    }

    @Test
    fun diff_weakestDsaTopicChange_detected() {
        val newDsaTopic = SuggestedDsaTopicSnapshot(category = "Trees", topic = "Binary Search Tree", confidence = 2, revisionStatus = "IN_PROGRESS")
        val newSnapshot = baseSnapshot.copy(suggestedDsaTopic = newDsaTopic)

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertFalse(delta.isEmpty)
        assertTrue(delta.dsaTopicDelta.changed)
        assertEquals("Graphs", delta.dsaTopicDelta.oldTopic?.category)
        assertEquals("Trees", delta.dsaTopicDelta.newTopic?.category)
    }

    @Test
    fun diff_projectActionChange_detected() {
        val newProjectAction = SuggestedProjectActionSnapshot(project = "Student OS", action = "Run unit tests")
        val newSnapshot = baseSnapshot.copy(suggestedProjectAction = newProjectAction)

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertFalse(delta.isEmpty)
        assertTrue(delta.projectActionDelta.changed)
        assertEquals("Implement SnapshotDiffer", delta.projectActionDelta.oldAction?.action)
        assertEquals("Run unit tests", delta.projectActionDelta.newAction?.action)
    }

    @Test
    fun diff_scoreChange_detected() {
        val newSnapshot = baseSnapshot.copy(score = ScoreSnapshot(target = 80, actual = 35))

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertFalse(delta.isEmpty)
        assertTrue(delta.scoreDelta.targetChanged)
        assertEquals(65, delta.scoreDelta.oldTarget)
        assertEquals(80, delta.scoreDelta.newTarget)
        assertTrue(delta.scoreDelta.actualChanged)
        assertEquals(20, delta.scoreDelta.oldActual)
        assertEquals(35, delta.scoreDelta.newActual)
    }

    @Test
    fun diff_cpRatingChange_detected() {
        val newSnapshot = baseSnapshot.copy(
            cpSummary = baseSnapshot.cpSummary.copy(codechefRating = 1850)
        )

        val delta = snapshotDiffer.diff(baseSnapshot, newSnapshot)
        assertFalse(delta.isEmpty)
        assertTrue(delta.cpSummaryDelta.ratingChanged)
        assertTrue(delta.cpSummaryDelta.codechefChanged)
        assertFalse(delta.cpSummaryDelta.codeforcesChanged)
    }

    @Test
    fun diff_deterministicOutput_forIdenticalInputs() {
        val delta1 = snapshotDiffer.diff(baseSnapshot, baseSnapshot)
        val delta2 = snapshotDiffer.diff(baseSnapshot, baseSnapshot)

        assertEquals(delta1, delta2)
    }
}
