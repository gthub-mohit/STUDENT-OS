package com.studentos.feature.intelligence.presentation

import com.studentos.core.database.dao.ActivityLogDao
import com.studentos.core.database.dao.CommitmentDao
import com.studentos.core.database.dao.GoalDao
import com.studentos.core.database.dao.JournalDao
import com.studentos.feature.intelligence.domain.model.ExtractedActivityDto
import com.studentos.feature.intelligence.domain.model.ExtractionResponse
import com.studentos.feature.intelligence.domain.model.ExtractionResult
import com.studentos.feature.intelligence.domain.usecase.ConfirmAndPersistActivitiesUseCase
import com.studentos.feature.intelligence.domain.usecase.ExtractJournalUseCase
import com.studentos.feature.intelligence.domain.usecase.PersistenceResult
import com.studentos.feature.intelligence.presentation.viewmodel.FocusJournalViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FocusJournalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val extractJournalUseCase: ExtractJournalUseCase = mockk()
    private val confirmAndPersistActivitiesUseCase: ConfirmAndPersistActivitiesUseCase = mockk()
    private val activityLogDao: ActivityLogDao = mockk()
    private val commitmentDao: CommitmentDao = mockk()
    private val journalDao: JournalDao = mockk()
    private val goalDao: GoalDao = mockk()

    private lateinit var viewModel: FocusJournalViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { activityLogDao.observeLogsBetween(any(), any()) } returns flowOf(emptyList())
        every { commitmentDao.observePendingCommitments() } returns flowOf(emptyList())
        every { journalDao.observeAllEntries() } returns flowOf(emptyList())
        every { goalDao.observeActiveGoals() } returns flowOf(emptyList())

        viewModel = FocusJournalViewModel(
            extractJournalUseCase = extractJournalUseCase,
            confirmAndPersistActivitiesUseCase = confirmAndPersistActivitiesUseCase,
            activityLogDao = activityLogDao,
            commitmentDao = commitmentDao,
            journalDao = journalDao,
            goalDao = goalDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `input text updates rawInput in state`() {
        viewModel.onRawInputChanged("Studied binary trees")
        assertEquals("Studied binary trees", viewModel.uiState.value.rawInput)
    }

    @Test
    fun `successful extraction shows preview with extracted items`() = runTest {
        val extractedResponse = ExtractionResponse(
            summary = "Practiced binary tree traversals.",
            activities = listOf(
                ExtractedActivityDto(
                    title = "Binary tree traversals",
                    activityType = "DSA",
                    metricDelta = 2.0,
                    durationMinutes = 40
                )
            ),
            commitments = emptyList()
        )

        coEvery { extractJournalUseCase("Solved 2 trees in 40m") } returns ExtractionResult.Success(
            response = extractedResponse,
            journalEntryId = 88L
        )

        viewModel.onRawInputChanged("Solved 2 trees in 40m")
        viewModel.submitForExtraction()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.rawInput)
        assertNotNull(state.preview)
        val preview = state.preview!!
        assertEquals(88L, preview.journalEntryId)
        assertEquals("Practiced binary tree traversals.", preview.summary)
        assertEquals(1, preview.activities.size)
        assertEquals("Binary tree traversals", preview.activities[0].title)
    }

    @Test
    fun `confirmPreview calls persistence usecase and clears preview`() = runTest {
        val extractedResponse = ExtractionResponse(
            summary = "Summary",
            activities = listOf(
                ExtractedActivityDto(title = "Task A", activityType = "GENERAL")
            )
        )
        coEvery { extractJournalUseCase(any()) } returns ExtractionResult.Success(
            response = extractedResponse,
            journalEntryId = 15L
        )
        coEvery {
            confirmAndPersistActivitiesUseCase(any(), any(), any(), any(), any(), any())
        } returns PersistenceResult(persistedActivitiesCount = 1, persistedCommitmentsCount = 0, skippedDuplicatesCount = 0)

        viewModel.onRawInputChanged("Did Task A")
        viewModel.submitForExtraction()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.preview)

        viewModel.confirmPreview()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.preview)
        coVerify(exactly = 1) {
            confirmAndPersistActivitiesUseCase(
                journalEntryId = 15L,
                confirmedActivities = any(),
                confirmedCommitments = any(),
                summary = "Summary",
                rawInput = any(),
                timestamp = any()
            )
        }
    }

    @Test
    fun `discardPreview clears preview without persisting structured records`() = runTest {
        val extractedResponse = ExtractionResponse(summary = "Summary", activities = emptyList())
        coEvery { extractJournalUseCase(any()) } returns ExtractionResult.Success(
            response = extractedResponse,
            journalEntryId = 20L
        )

        viewModel.onRawInputChanged("Log")
        viewModel.submitForExtraction()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.preview)

        viewModel.discardPreview()
        assertNull(viewModel.uiState.value.preview)
        coVerify(exactly = 0) { confirmAndPersistActivitiesUseCase(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `toggleCommitmentComplete marks commitment completed in DAO`() = runTest {
        coEvery { commitmentDao.updateStatus(101L, "COMPLETED", any()) } returns Unit

        viewModel.toggleCommitmentComplete(101L)
        advanceUntilIdle()

        coVerify(exactly = 1) { commitmentDao.updateStatus(101L, "COMPLETED", any()) }
    }

    @Test
    fun `database error in flow sets single errorMessage and allows retry`() = runTest {
        val failingActivityLogDao: ActivityLogDao = mockk()
        val failingCommitmentDao: CommitmentDao = mockk()
        val failingJournalDao: JournalDao = mockk()
        val failingGoalDao: GoalDao = mockk()

        every { failingActivityLogDao.observeLogsBetween(any(), any()) } returns kotlinx.coroutines.flow.flow {
            throw IllegalStateException("Database query failed")
        }
        every { failingCommitmentDao.observePendingCommitments() } returns kotlinx.coroutines.flow.flow {
            throw IllegalStateException("Database query failed")
        }
        every { failingJournalDao.observeAllEntries() } returns kotlinx.coroutines.flow.flow {
            throw IllegalStateException("Database query failed")
        }
        every { failingGoalDao.observeActiveGoals() } returns flowOf(emptyList())

        val testVm = FocusJournalViewModel(
            extractJournalUseCase = extractJournalUseCase,
            confirmAndPersistActivitiesUseCase = confirmAndPersistActivitiesUseCase,
            activityLogDao = failingActivityLogDao,
            commitmentDao = failingCommitmentDao,
            journalDao = failingJournalDao,
            goalDao = failingGoalDao
        )

        advanceUntilIdle()

        // Should present a single, clean contextual error
        assertNotNull(testVm.uiState.value.errorMessage)
        org.junit.Assert.assertTrue(testVm.uiState.value.errorMessage!!.contains("Unable to load local"))

        // Clear messages
        testVm.clearMessages()
        assertNull(testVm.uiState.value.errorMessage)

        // Retry database load
        testVm.retryDatabaseLoad()
        advanceUntilIdle()
        assertNotNull(testVm.uiState.value.errorMessage)
    }

    @Test
    fun `saving raw note only does not fabricate activity logs or increment accomplishments`() = runTest {
        val sampleNote = "solved leetcode questions on Array , started machine learning with pandas and numpy"
        coEvery { journalDao.insert(any()) } returns 42L

        viewModel.onRawInputChanged(sampleNote)
        viewModel.saveRawNoteOnly()
        advanceUntilIdle()

        // Verifies raw input is saved as raw note only
        coVerify(exactly = 1) {
            journalDao.insert(match {
                it.rawLog == sampleNote && it.structuredSummary == null
            })
        }

        // Verifies NO activity logs are fabricated or persisted without AI extraction and user confirmation
        coVerify(exactly = 0) {
            confirmAndPersistActivitiesUseCase(any(), any(), any(), any(), any(), any())
        }

        // Verifies input is cleared and guidance is given to user
        assertEquals("", viewModel.uiState.value.rawInput)
        val userMsg = viewModel.uiState.value.userMessage
        assertNotNull(userMsg)
        org.junit.Assert.assertTrue(userMsg!!.contains("Extract with AI"))
    }

    @Test
    fun `extractExistingJournalEntry extracts unextracted note and confirms activities reactively`() = runTest {
        val rawNoteEntry = com.studentos.core.database.entity.JournalEntryEntity(
            id = 55L,
            date = "2026-10-18",
            rawLog = "solved leetcode questions on Array , started machine learning with pandas and numpy",
            structuredSummary = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val extractedResponse = ExtractionResponse(
            summary = "Solved Array questions and started ML with pandas/numpy.",
            activities = listOf(
                ExtractedActivityDto(
                    title = "Solved LeetCode Array questions",
                    activityType = "DSA",
                    metricDelta = 5.0
                ),
                ExtractedActivityDto(
                    title = "Started ML with pandas and numpy",
                    activityType = "ML",
                    metricDelta = 1.0
                )
            ),
            commitments = emptyList()
        )

        coEvery { extractJournalUseCase(rawNoteEntry.rawLog, any()) } returns ExtractionResult.Success(
            response = extractedResponse,
            journalEntryId = 55L
        )

        coEvery {
            confirmAndPersistActivitiesUseCase(
                journalEntryId = 55L,
                confirmedActivities = any(),
                confirmedCommitments = any(),
                summary = any(),
                rawInput = any(),
                timestamp = any()
            )
        } returns PersistenceResult(persistedActivitiesCount = 2, persistedCommitmentsCount = 0, skippedDuplicatesCount = 0)

        // 1. User taps "Extract with AI" on the raw history item
        viewModel.extractExistingJournalEntry(rawNoteEntry)
        advanceUntilIdle()

        // Verify preview is populated with 2 activities and correct journalEntryId
        assertNotNull(viewModel.uiState.value.preview)
        val preview = viewModel.uiState.value.preview!!
        assertEquals(55L, preview.journalEntryId)
        assertEquals(2, preview.activities.size)

        // 2. User confirms extraction
        viewModel.confirmPreview()
        advanceUntilIdle()

        // Verify activities are persisted into ActivityLogEntity via use case
        coVerify(exactly = 1) {
            confirmAndPersistActivitiesUseCase(
                journalEntryId = 55L,
                confirmedActivities = match { it.size == 2 },
                confirmedCommitments = any(),
                summary = any(),
                rawInput = rawNoteEntry.rawLog,
                timestamp = any()
            )
        }
        assertNull(viewModel.uiState.value.preview)
    }
}
