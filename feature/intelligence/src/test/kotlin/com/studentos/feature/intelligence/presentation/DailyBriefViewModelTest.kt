package com.studentos.feature.intelligence.presentation

import androidx.lifecycle.SavedStateHandle
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.model.RecommendationCard
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import com.studentos.feature.intelligence.orchestrator.IntelligenceOrchestrator
import com.studentos.feature.intelligence.presentation.viewmodel.DailyBriefViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DailyBriefViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private val repository: DailyBriefRepository = mockk()
    private val orchestrator: IntelligenceOrchestrator = mockk()

    private lateinit var viewModel: DailyBriefViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_autoGenerates_whenTodayBriefDoesNotExist() = runTest {
        val todayLocalDate = LocalDate.of(2026, 8, 1)
        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(null)

        val mockBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 100
        )
        coEvery { orchestrator.generateMorningBrief(todayLocalDate) } returns mockBrief

        viewModel = DailyBriefViewModel(
            repository = repository,
            orchestrator = orchestrator,
            clock = clock,
            savedStateHandle = SavedStateHandle()
        )

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { orchestrator.generateMorningBrief(todayLocalDate) }
    }

    @Test
    fun initialState_doesNotAutoGenerate_whenTodayBriefAlreadyExists() = runTest {
        val existingBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 100
        )
        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(existingBrief)

        viewModel = DailyBriefViewModel(
            repository = repository,
            orchestrator = orchestrator,
            clock = clock,
            savedStateHandle = SavedStateHandle()
        )

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { orchestrator.generateMorningBrief(any()) }
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(existingBrief, state.dailyBrief)
    }

    @Test
    fun generateTodayBrief_invokesOrchestrator_andUpdatesUiState() = runTest {
        val todayLocalDate = LocalDate.of(2026, 8, 1)
        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(null)

        val cards = listOf(
            RecommendationCard(
                id = "1",
                title = "Attend Physics",
                description = "Attendance is at 65%",
                category = "ATTENDANCE",
                priority = 1,
                actionRoute = "weekly"
            )
        )
        val mockBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = Json.encodeToString(cards),
            guidanceSource = DailyBrief.GUIDANCE_SOURCE_LLM,
            scoreTarget = 100,
            scoreActual = 80
        )
        coEvery { orchestrator.generateMorningBrief(todayLocalDate) } returns mockBrief

        viewModel = DailyBriefViewModel(
            repository = repository,
            orchestrator = orchestrator,
            clock = clock,
            savedStateHandle = SavedStateHandle()
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateTodayBrief()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertFalse(state.isLoading)
        assertEquals(1, state.recommendations.size)
        assertEquals("Attend Physics", state.recommendations.first().title)
        assertEquals("weekly", state.recommendations.first().actionRoute)
    }

    @Test
    fun initialState_autoGenerates_whenLegacyMockBriefExistsForToday() = runTest {
        val todayLocalDate = LocalDate.of(2026, 8, 1)
        val legacyMockBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = "[]",
            llmGuidance = "Mock Brief Guidance: Prioritize upcoming deadlines\n[Prompt length: 83]",
            guidanceSource = "MOCK",
            scoreTarget = 100,
            scoreActual = 100
        )
        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(legacyMockBrief)

        val freshBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash456",
            briefJson = "[]",
            llmGuidance = "Prioritize upcoming deadlines and complete scheduled classes.",
            guidanceSource = DailyBrief.GUIDANCE_SOURCE_LLM,
            scoreTarget = 100,
            scoreActual = 100
        )
        coEvery { orchestrator.generateMorningBrief(todayLocalDate) } returns freshBrief

        viewModel = DailyBriefViewModel(
            repository = repository,
            orchestrator = orchestrator,
            clock = clock,
            savedStateHandle = SavedStateHandle()
        )

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { orchestrator.generateMorningBrief(todayLocalDate) }
    }
}
