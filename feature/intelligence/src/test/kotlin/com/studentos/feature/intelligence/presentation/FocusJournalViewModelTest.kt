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
}
