package com.studentos.feature.intelligence.generator

import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.feature.intelligence.domain.analyzer.AssignmentAnalyzer
import com.studentos.feature.intelligence.domain.analyzer.AttendanceAnalyzer
import com.studentos.feature.intelligence.domain.analyzer.CodingAnalyzer
import com.studentos.feature.intelligence.domain.analyzer.ProjectAnalyzerStub
import com.studentos.feature.intelligence.domain.generator.DailyBriefGenerator
import com.studentos.feature.intelligence.domain.model.fact.AssignmentFact
import com.studentos.feature.intelligence.domain.model.fact.AttendanceFact
import com.studentos.feature.intelligence.domain.model.fact.IntelligenceFacts
import com.studentos.feature.intelligence.domain.service.PriorityScoringEngine
import com.studentos.feature.intelligence.domain.service.RecommendationEngine
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DailyBriefGeneratorTest {

    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private val subjectDao: SubjectDao = mockk(relaxed = true)
    private val timetableSlotDao: TimetableSlotDao = mockk(relaxed = true)
    private val classEventDao: ClassEventDao = mockk(relaxed = true)
    private val assignmentDao: AssignmentDao = mockk(relaxed = true)
    private val cpProfileDao: CpProfileDao = mockk(relaxed = true)
    private val cpContestDao: CpContestDao = mockk(relaxed = true)
    private val dsaTopicDao: DsaTopicDao = mockk(relaxed = true)

    private lateinit var attendanceAnalyzer: AttendanceAnalyzer
    private lateinit var assignmentAnalyzer: AssignmentAnalyzer
    private lateinit var codingAnalyzer: CodingAnalyzer
    private lateinit var projectAnalyzerStub: ProjectAnalyzerStub
    private lateinit var scoringEngine: PriorityScoringEngine
    private lateinit var recommendationEngine: RecommendationEngine
    private lateinit var generator: DailyBriefGenerator

    @Before
    fun setUp() {
        attendanceAnalyzer = AttendanceAnalyzer(subjectDao, timetableSlotDao, classEventDao)
        assignmentAnalyzer = AssignmentAnalyzer(assignmentDao, clock)
        codingAnalyzer = CodingAnalyzer(cpProfileDao, cpContestDao, dsaTopicDao, clock)
        projectAnalyzerStub = ProjectAnalyzerStub()
        scoringEngine = PriorityScoringEngine()
        recommendationEngine = RecommendationEngine(scoringEngine)

        generator = DailyBriefGenerator(
            attendanceAnalyzer = attendanceAnalyzer,
            assignmentAnalyzer = assignmentAnalyzer,
            codingAnalyzer = codingAnalyzer,
            projectAnalyzerStub = projectAnalyzerStub,
            recommendationEngine = recommendationEngine,
            scoringEngine = scoringEngine,
            clock = clock
        )

        coEvery { classEventDao.getAllAttendanceSummaries() } returns flowOf(emptyList())
        coEvery { subjectDao.getActiveSubjects() } returns flowOf(emptyList())
        coEvery { timetableSlotDao.getAllSlots() } returns flowOf(emptyList())
        coEvery { assignmentDao.getAssignmentsByStatus("PENDING") } returns flowOf(emptyList())
        coEvery { cpProfileDao.getAllProfiles() } returns flowOf(emptyList())
        coEvery { cpContestDao.getUpcomingContests(any(), any()) } returns emptyList()
        coEvery { dsaTopicDao.getSuggestedTopic() } returns null
    }

    @Test
    fun generateBrief_orchestratesAnalyzers_and_includesSchemaVersion() = runTest {
        val brief = generator.generateBrief("2026-08-01")

        assertNotNull(brief)
        assertEquals("2026-08-01", brief.date)
        assertTrue(brief.jsonSnapshot.contains("\"schemaVersion\":\"1\""))
        assertEquals(fixedInstant.toEpochMilli(), brief.generatedAt)
    }

    @Test
    fun recommendationEngine_sortsCardsByPriority() = runTest {
        val facts = IntelligenceFacts(
            date = "2026-08-01",
            attendance = AttendanceFact(
                todaySlots = listOf(
                    com.studentos.feature.intelligence.domain.model.fact.TimetableSlotFact(
                        slotId = 1, subjectId = 10, subjectName = "Maths", startTime = "09:00", endTime = "10:00"
                    )
                )
            ),
            assignments = AssignmentFact(
                overdueCount = 1,
                urgentAssignments = listOf(
                    com.studentos.feature.intelligence.domain.model.fact.AssignmentItemFact(
                        id = 1, title = "OS Lab", deadlineEpochMs = fixedInstant.toEpochMilli() - 1000, isOverdue = true, isUrgent = true
                    )
                )
            )
        )

        val cards = recommendationEngine.generateRecommendations(facts)
        assertTrue(cards.isNotEmpty())
        assertEquals(1, cards.first().priority)
    }
}
