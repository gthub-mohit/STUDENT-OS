package com.studentos.core.intelligence.snapshot

import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.DsaCategoryDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.database.entity.DsaCategoryEntity
import com.studentos.core.database.entity.DsaTopicEntity
import com.studentos.core.database.entity.ProjectEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.core.database.relation.ProjectWithNextAction
import com.studentos.core.database.relation.SubjectAttendanceSummary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SnapshotBuilderTest {

    private val subjectDao: SubjectDao = mockk()
    private val timetableSlotDao: TimetableSlotDao = mockk()
    private val classEventDao: ClassEventDao = mockk()
    private val assignmentDao: AssignmentDao = mockk()
    private val dsaTopicDao: DsaTopicDao = mockk()
    private val dsaCategoryDao: DsaCategoryDao = mockk()
    private val projectDao: ProjectDao = mockk()
    private val cpProfileDao: CpProfileDao = mockk()
    private val settingsDao: SettingsDao = mockk()

    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private lateinit var snapshotBuilder: SnapshotBuilder

    @Before
    fun setUp() {
        snapshotBuilder = SnapshotBuilder(
            subjectDao,
            timetableSlotDao,
            classEventDao,
            assignmentDao,
            dsaTopicDao,
            dsaCategoryDao,
            projectDao,
            cpProfileDao,
            settingsDao,
            clock
        )
    }

    @Test
    fun build_handlesEmptyRepositories_gracefully() = runTest {
        coEvery { settingsDao.get(any()) } returns null
        coEvery { timetableSlotDao.getAllSlots() } returns flowOf(emptyList())
        coEvery { subjectDao.getActiveSubjects() } returns flowOf(emptyList())
        coEvery { classEventDao.getAllAttendanceSummaries() } returns flowOf(emptyList())
        coEvery { assignmentDao.getUrgentAssignments(any()) } returns emptyList()
        coEvery { dsaTopicDao.getSuggestedTopic() } returns null
        coEvery { projectDao.getProjectsWithNextAction() } returns emptyList()
        coEvery { cpProfileDao.getProfilesForSnapshot() } returns emptyList()

        val snapshot = snapshotBuilder.build("2026-08-01")

        assertEquals(1, snapshot.snapshotVersion)
        assertEquals("2026-08-01", snapshot.date)
        assertTrue(snapshot.classesToday.isEmpty())
        assertTrue(snapshot.attendanceWarnings.isEmpty())
        assertTrue(snapshot.assignmentsUrgent.isEmpty())
        assertTrue(snapshot.freeSlots.isEmpty())
        assertNull(snapshot.suggestedDsaTopic)
        assertNull(snapshot.suggestedProjectAction)
        assertEquals(0, snapshot.score.target)
    }

    @Test
    fun build_aggregatesPopulatedFactsCorrectly() = runTest {
        coEvery { settingsDao.get("student_name") } returns "Alex"
        coEvery { settingsDao.get("tone_preference") } returns "concise"
        coEvery { settingsDao.get("attendance_threshold") } returns "75.0"

        val subject = SubjectEntity(id = 1, name = "Data Structures", archivedAt = null)
        coEvery { subjectDao.getActiveSubjects() } returns flowOf(listOf(subject))

        val attendanceSummary = SubjectAttendanceSummary(
            subjectId = 1,
            subjectName = "Data Structures",
            presentCount = 5,
            absentCount = 5,
            cancelledCount = 0,
            holidayCount = 0,
            extraPresentCount = 0,
            totalHeldCount = 10,
            percentage = 50.0
        )
        coEvery { classEventDao.getAllAttendanceSummaries() } returns flowOf(listOf(attendanceSummary))

        val slot1 = TimetableSlotEntity(
            id = 10,
            subjectId = 1,
            dayOfWeek = 6,
            startTime = "09:00",
            endTime = "10:00",
            location = "Room 101",
            weekParity = null,
            validFrom = 0L,
            validUntil = null
        )
        val slot2 = TimetableSlotEntity(
            id = 11,
            subjectId = 1,
            dayOfWeek = 6,
            startTime = "11:00",
            endTime = "12:00",
            location = "Room 102",
            weekParity = null,
            validFrom = 0L,
            validUntil = null
        )
        coEvery { timetableSlotDao.getAllSlots() } returns flowOf(listOf(slot1, slot2))

        val dueEpoch = fixedInstant.plusSeconds(7200).toEpochMilli() // +2 hours
        val assignment = AssignmentEntity(
            id = 100,
            subjectId = 1,
            title = "Lab 1",
            description = "Desc",
            deadline = dueEpoch,
            priority = "HIGH",
            status = "PENDING",
            createdAt = 1000L
        )
        coEvery { assignmentDao.getUrgentAssignments(any()) } returns listOf(assignment)

        val topic = DsaTopicEntity(id = 20, categoryId = 2, name = "Graph BFS", confidenceLevel = 1, revisionStatus = "NOT_STARTED")
        val category = DsaCategoryEntity(id = 2, name = "Graphs")
        coEvery { dsaTopicDao.getSuggestedTopic() } returns topic
        coEvery { dsaCategoryDao.getCategoryById(2) } returns flowOf(category)

        val projectEntity = ProjectEntity(id = 300, title = "Student OS", lastActivityAt = 1000L)
        val projectAction = ProjectWithNextAction(
            project = projectEntity,
            nextActionId = 301,
            nextActionTitle = "Build SnapshotBuilder",
            isNextAction = true,
            isParallel = false,
            completedAt = null,
            sortOrder = 0
        )
        coEvery { projectDao.getProjectsWithNextAction() } returns listOf(projectAction)

        val cpProfile = CpProfileEntity(id = 1, platform = "CODECHEF", handle = "alex", currentRating = 1800, lastSyncedAt = 10000L)
        coEvery { cpProfileDao.getProfilesForSnapshot() } returns listOf(cpProfile)

        val startTime = System.currentTimeMillis()
        val snapshot = snapshotBuilder.build("2026-08-01")
        val duration = System.currentTimeMillis() - startTime

        assertTrue("Snapshot building took too long: ${duration}ms", duration < 2000)
        assertEquals("Alex", snapshot.studentContext.name)
        assertEquals("concise", snapshot.studentContext.tonePreference)
        assertEquals(2, snapshot.classesToday.size)
        assertEquals(1, snapshot.attendanceWarnings.size)
        assertEquals(50.0, snapshot.attendanceWarnings.first().percentage, 0.01)
        assertEquals(1, snapshot.assignmentsUrgent.size)
        assertEquals(1, snapshot.freeSlots.size)
        assertEquals(60, snapshot.freeSlots.first().durationMinutes)

        assertNotNull(snapshot.suggestedDsaTopic)
        assertEquals("Graph BFS", snapshot.suggestedDsaTopic?.topic)

        assertNotNull(snapshot.suggestedProjectAction)
        assertEquals("Build SnapshotBuilder", snapshot.suggestedProjectAction?.action)

        assertEquals(1800, snapshot.cpSummary.codechefRating)
        // Score target = 2 classes * 10 + 1 urgent assignment * 20 + 1 project action * 15 + 1 dsa * 10 = 65
        assertEquals(65, snapshot.score.target)
    }
}
