package com.studentos.feature.intelligence.domain

import com.studentos.core.database.dao.CommitmentDao
import com.studentos.core.database.dao.GoalDao
import com.studentos.core.database.dao.JournalDao
import com.studentos.core.database.entity.CommitmentEntity
import com.studentos.core.database.entity.GoalEntity
import com.studentos.core.database.entity.JournalEntryEntity
import com.studentos.core.intelligence.model.FailureReason
import com.studentos.core.intelligence.model.LLMResult
import com.studentos.core.intelligence.provider.OmniRouteProvider
import com.studentos.feature.intelligence.domain.model.ExtractionResult
import com.studentos.feature.intelligence.domain.usecase.ExtractJournalUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExtractJournalUseCaseTest {

    private val omniRouteProvider: OmniRouteProvider = mockk()
    private val journalDao: JournalDao = mockk()
    private val goalDao: GoalDao = mockk()
    private val commitmentDao: CommitmentDao = mockk()

    private lateinit var useCase: ExtractJournalUseCase

    @Before
    fun setup() {
        useCase = ExtractJournalUseCase(
            omniRouteProvider = omniRouteProvider,
            journalDao = journalDao,
            goalDao = goalDao,
            commitmentDao = commitmentDao
        )
    }

    @Test
    fun `raw journal is saved locally before calling OmniRoute gateway`() = runTest {
        val entrySlot = slot<JournalEntryEntity>()
        coEvery { journalDao.insert(capture(entrySlot)) } returns 42L
        coEvery { goalDao.getActiveGoals() } returns emptyList()
        coEvery { commitmentDao.getPendingCommitments() } returns emptyList()
        coEvery { omniRouteProvider.generateCompletion(any(), any()) } returns LLMResult.Failure(
            reason = FailureReason.OFFLINE,
            message = "No internet connection"
        )

        val result = useCase("Solved 2 questions on binary search")

        // 1. Verify journal note was persisted in Room
        coVerify(exactly = 1) { journalDao.insert(any()) }
        assertEquals("Solved 2 questions on binary search", entrySlot.captured.rawLog)

        // 2. Verify error returned preserves journal entry id and marks offline
        assertTrue(result is ExtractionResult.Error)
        val errorResult = result as ExtractionResult.Error
        assertEquals(42L, errorResult.journalEntryId)
        assertTrue(errorResult.isOffline)
    }

    @Test
    fun `retrieves active goals and commitments from Room and includes in prompt`() = runTest {
        coEvery { journalDao.insert(any()) } returns 101L
        coEvery { goalDao.getActiveGoals() } returns listOf(
            GoalEntity(
                id = 5L,
                title = "200 LeetCode Questions",
                category = "DSA",
                metricType = "COUNT",
                unit = "questions",
                startValue = 0.0,
                targetValue = 200.0,
                startDate = 1000L,
                targetDate = 5000L,
                createdAt = 1000L,
                updatedAt = 1000L
            )
        )
        coEvery { commitmentDao.getPendingCommitments() } returns listOf(
            CommitmentEntity(
                id = 8L,
                title = "Compiler assignment 3",
                category = "ACADEMIC",
                priority = "HIGH",
                createdAt = 1000L
            )
        )

        val promptSlot = slot<String>()
        val jsonOutput = """
            {
              "summary": "Solved 3 questions for LeetCode goal.",
              "activities": [
                {
                  "title": "Solved 3 tree problems",
                  "activityType": "DSA",
                  "metricDelta": 3.0,
                  "durationMinutes": 60,
                  "matchedGoalId": 5
                }
              ],
              "commitments": []
            }
        """.trimIndent()

        coEvery {
            omniRouteProvider.generateCompletion(capture(promptSlot), any())
        } returns LLMResult.Success(text = jsonOutput, tokenCount = 120)

        val result = useCase("Solved 3 tree problems for LeetCode goal in 60m")

        assertTrue(result is ExtractionResult.Success)
        val success = result as ExtractionResult.Success
        assertEquals(101L, success.journalEntryId)
        assertEquals(1, success.response.activities.size)
        assertEquals(5L, success.response.activities[0].matchedGoalId)

        // Verify the prompt sent to OmniRoute included the Room goals context
        val sentPrompt = promptSlot.captured
        assertTrue(sentPrompt.contains("200 LeetCode Questions"))
        assertTrue(sentPrompt.contains("Compiler assignment 3"))
    }

    @Test
    fun `malformed AI response returns error without losing journal entry id`() = runTest {
        coEvery { journalDao.insert(any()) } returns 77L
        coEvery { goalDao.getActiveGoals() } returns emptyList()
        coEvery { commitmentDao.getPendingCommitments() } returns emptyList()
        coEvery { omniRouteProvider.generateCompletion(any(), any()) } returns LLMResult.Success(
            text = "Sorry, I am an AI and cannot format this as JSON right now.",
            tokenCount = 50
        )

        val result = useCase("Quick reflection on today's classes")

        assertTrue(result is ExtractionResult.Error)
        val error = result as ExtractionResult.Error
        assertEquals(77L, error.journalEntryId)
        assertTrue(error.message.contains("Could not parse AI response"))
    }
}
