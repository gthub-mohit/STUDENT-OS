package com.studentos.feature.intelligence.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.model.RecommendationCard
import com.studentos.feature.intelligence.domain.usecase.GenerateDailyBriefUseCase
import com.studentos.feature.intelligence.domain.usecase.GetDailyBriefUseCase
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
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DailyBriefViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private val getDailyBriefUseCase: GetDailyBriefUseCase = mockk()
    private val generateDailyBriefUseCase: GenerateDailyBriefUseCase = mockk()

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
        coEvery { getDailyBriefUseCase("2026-08-01") } returns flowOf(null)

        val mockBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 100
        )
        coEvery { generateDailyBriefUseCase("2026-08-01") } returns mockBrief

        viewModel = DailyBriefViewModel(
            getDailyBriefUseCase = getDailyBriefUseCase,
            generateDailyBriefUseCase = generateDailyBriefUseCase,
            clock = clock,
            savedStateHandle = SavedStateHandle()
        )

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { generateDailyBriefUseCase("2026-08-01") }
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
        coEvery { getDailyBriefUseCase("2026-08-01") } returns flowOf(existingBrief)

        viewModel = DailyBriefViewModel(
            getDailyBriefUseCase = getDailyBriefUseCase,
            generateDailyBriefUseCase = generateDailyBriefUseCase,
            clock = clock,
            savedStateHandle = SavedStateHandle()
        )

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { generateDailyBriefUseCase(any()) }
        assertEquals("2026-08-01", viewModel.uiState.value.todayDate)
        assertNotNull(viewModel.uiState.value.dailyBrief)
    }

    @Test
    fun generateTodayBrief_invokesGenerator_and_updatesStateWithRecommendations() = runTest {
        coEvery { getDailyBriefUseCase("2026-08-01") } returns flowOf(null)

        val cards = listOf(
            RecommendationCard(
                id = "att_low_1",
                title = "Low Attendance",
                description = "Attend Math class",
                category = "ATTENDANCE",
                priority = 1,
                actionRoute = "weekly"
            )
        )
        val jsonCards = Json.encodeToString(cards)

        val mockBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = jsonCards,
            scoreTarget = 100,
            scoreActual = 100
        )

        coEvery { generateDailyBriefUseCase("2026-08-01") } returns mockBrief

        viewModel = DailyBriefViewModel(
            getDailyBriefUseCase = getDailyBriefUseCase,
            generateDailyBriefUseCase = generateDailyBriefUseCase,
            clock = clock,
            savedStateHandle = SavedStateHandle()
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.generateTodayBrief()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGenerating)
        assertFalse(state.isEmpty)
        assertNotNull(state.dailyBrief)
        assertEquals(1, state.recommendations.size)
        assertEquals("Low Attendance", state.recommendations.first().title)
    }
}
