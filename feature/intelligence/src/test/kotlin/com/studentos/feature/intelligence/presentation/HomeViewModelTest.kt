package com.studentos.feature.intelligence.presentation

import com.studentos.core.events.AppEventBus
import com.studentos.feature.intelligence.domain.model.ComingUpItem
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.model.TodayFocusItem
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import com.studentos.feature.intelligence.domain.usecase.GetComingUpUseCase
import com.studentos.feature.intelligence.domain.usecase.GetTodayFocusUseCase
import com.studentos.feature.intelligence.domain.usecase.ToggleFocusItemUseCase
import com.studentos.feature.intelligence.orchestrator.IntelligenceOrchestrator
import com.studentos.feature.intelligence.presentation.viewmodel.HomeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private val getTodayFocusUseCase: GetTodayFocusUseCase = mockk()
    private val getComingUpUseCase: GetComingUpUseCase = mockk()
    private val toggleFocusItemUseCase: ToggleFocusItemUseCase = mockk(relaxed = true)
    private val dailyBriefRepository: DailyBriefRepository = mockk()
    private val orchestrator: IntelligenceOrchestrator = mockk()
    private val appEventBus: AppEventBus = mockk()

    private val eventsFlow = MutableSharedFlow<com.studentos.core.events.AppEvent>(extraBufferCapacity = 64)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { appEventBus.events } returns eventsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoad_derivesSingleSourceOfTruthProgress_fromFocusItems() = runTest {
        val focusItems = listOf(
            TodayFocusItem("1", "Finish Mechanics assignment", null, "ASSIGNMENT", isCompleted = true, entityId = 1L),
            TodayFocusItem("2", "Attend Digital Systems", null, "ATTENDANCE", isCompleted = false, entityId = 2L),
            TodayFocusItem("3", "Practice DSA", null, "DSA", isCompleted = false, entityId = 3L)
        )
        val comingUpItems = listOf(
            ComingUpItem("c1", "Mechanics Assignment", "Due tomorrow", "ASSIGNMENT", "assignments/list", 1000L)
        )

        every { getTodayFocusUseCase() } returns flowOf(focusItems)
        every { getComingUpUseCase() } returns flowOf(comingUpItems)
        coEvery { dailyBriefRepository.getBriefForDate("2026-08-01") } returns flowOf(null)

        viewModel = HomeViewModel(
            getTodayFocusUseCase,
            getComingUpUseCase,
            toggleFocusItemUseCase,
            dailyBriefRepository,
            orchestrator,
            appEventBus,
            clock
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.totalPrioritiesCount)
        assertEquals(1, state.completedPrioritiesCount)
        assertEquals(1f / 3f, state.progressBarValue, 0.01f)
        assertEquals(3, state.focusItems.size)
        assertEquals(1, state.comingUpItems.size)
    }

    @Test
    fun initialLoad_withZeroPriorities_setsZeroProgressAndAppropriateGoalSummary() = runTest {
        every { getTodayFocusUseCase() } returns flowOf(emptyList())
        every { getComingUpUseCase() } returns flowOf(emptyList())
        coEvery { dailyBriefRepository.getBriefForDate("2026-08-01") } returns flowOf(null)

        viewModel = HomeViewModel(
            getTodayFocusUseCase,
            getComingUpUseCase,
            toggleFocusItemUseCase,
            dailyBriefRepository,
            orchestrator,
            appEventBus,
            clock
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.totalPrioritiesCount)
        assertEquals(0, state.completedPrioritiesCount)
        assertEquals(0f, state.progressBarValue, 0.01f)
        assertEquals("Tap to view Daily Brief", state.todayGoalSummary)
    }

    @Test
    fun toggleFocusItem_invokesToggleUseCase() = runTest {
        every { getTodayFocusUseCase() } returns flowOf(emptyList())
        every { getComingUpUseCase() } returns flowOf(emptyList())
        coEvery { dailyBriefRepository.getBriefForDate("2026-08-01") } returns flowOf(null)

        viewModel = HomeViewModel(
            getTodayFocusUseCase,
            getComingUpUseCase,
            toggleFocusItemUseCase,
            dailyBriefRepository,
            orchestrator,
            appEventBus,
            clock
        )

        val item = TodayFocusItem("1", "Finish Mechanics assignment", null, "ASSIGNMENT", isCompleted = false, entityId = 10L)
        viewModel.toggleFocusItem(item)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { toggleFocusItemUseCase(item) }
    }

    @Test
    fun generateTodayBrief_invokesOrchestrator() = runTest {
        val todayLocalDate = LocalDate.of(2026, 8, 1)
        val mockBrief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash123",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 100
        )

        every { getTodayFocusUseCase() } returns flowOf(emptyList())
        every { getComingUpUseCase() } returns flowOf(emptyList())
        coEvery { dailyBriefRepository.getBriefForDate("2026-08-01") } returns flowOf(null)
        coEvery { orchestrator.generateMorningBrief(todayLocalDate) } returns mockBrief

        viewModel = HomeViewModel(
            getTodayFocusUseCase,
            getComingUpUseCase,
            toggleFocusItemUseCase,
            dailyBriefRepository,
            orchestrator,
            appEventBus,
            clock
        )

        viewModel.generateTodayBrief()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { orchestrator.generateMorningBrief(todayLocalDate) }
    }
}
