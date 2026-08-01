package com.studentos.feature.intelligence.domain.generator

import com.studentos.feature.intelligence.domain.analyzer.AssignmentAnalyzer
import com.studentos.feature.intelligence.domain.analyzer.AttendanceAnalyzer
import com.studentos.feature.intelligence.domain.analyzer.CodingAnalyzer
import com.studentos.feature.intelligence.domain.analyzer.ProjectAnalyzerStub
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.model.fact.IntelligenceFacts
import com.studentos.feature.intelligence.domain.service.PriorityScoringEngine
import com.studentos.feature.intelligence.domain.service.RecommendationEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.time.Clock
import javax.inject.Inject

class DailyBriefGenerator @Inject constructor(
    private val attendanceAnalyzer: AttendanceAnalyzer,
    private val assignmentAnalyzer: AssignmentAnalyzer,
    private val codingAnalyzer: CodingAnalyzer,
    private val projectAnalyzerStub: ProjectAnalyzerStub,
    private val recommendationEngine: RecommendationEngine,
    private val scoringEngine: PriorityScoringEngine,
    private val clock: Clock
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateBrief(todayDate: String): DailyBrief = coroutineScope {
        val attendanceDeferred = async { attendanceAnalyzer.analyze(todayDate) }
        val assignmentDeferred = async { assignmentAnalyzer.analyze(todayDate) }
        val codingDeferred = async { codingAnalyzer.analyze(todayDate) }
        val projectDeferred = async { projectAnalyzerStub.analyze(todayDate) }

        val attendanceFact = attendanceDeferred.await()
        val assignmentFact = assignmentDeferred.await()
        val codingFact = codingDeferred.await()
        val projectFact = projectDeferred.await()

        val facts = IntelligenceFacts(
            date = todayDate,
            attendance = attendanceFact,
            assignments = assignmentFact,
            coding = codingFact,
            project = projectFact
        )

        val recommendations = recommendationEngine.generateRecommendations(facts)
        val targetScore = scoringEngine.calculateTargetScore(facts)

        val jsonSnapshot = json.encodeToString(mapOf("schemaVersion" to "1", "date" to todayDate))
        val briefJson = json.encodeToString(recommendations)
        val hash = sha256("$todayDate:$briefJson")
        val nowMs = clock.millis()

        DailyBrief(
            date = todayDate,
            jsonSnapshot = jsonSnapshot,
            snapshotHash = hash,
            briefJson = briefJson,
            llmGuidance = null,
            guidanceSource = DailyBrief.GUIDANCE_SOURCE_DETERMINISTIC,
            scoreTarget = targetScore,
            scoreActual = targetScore,
            generatedAt = nowMs,
            guidanceUpdatedAt = nowMs
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
