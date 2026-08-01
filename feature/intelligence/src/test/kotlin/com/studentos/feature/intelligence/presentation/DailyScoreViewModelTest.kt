package com.studentos.feature.intelligence.presentation

import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import com.studentos.feature.intelligence.presentation.viewmodel.DailyScoreViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DailyScoreViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private val repository: DailyBriefRepository = mockk()
    private val appEventBus: AppEventBus = mockk()
    private val eventsFlow = MutableSharedFlow<AppEvent>(extraBufferCapacity = 64)

    private lateinit var viewModel: DailyScoreViewModel

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
    fun loadScoreForToday_updatesUiStateWithBriefScore() = runTest {
        val brief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash1",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 75
        )
        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(brief)

        viewModel = DailyScoreViewModel(repository, appEventBus, clock)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(100, state.targetScore)
        assertEquals(75, state.currentScore)
        assertEquals(75f, state.progressPercentage, 0.01f)
        assertEquals(0.75f, state.progressBarValue, 0.01f)
        assertEquals(25, state.remainingScore)
    }

    @Test
    fun eventUpdatesScore_reloadsBriefScore() = runTest {
        val briefInitial = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash1",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 50
        )
        val briefUpdated = briefInitial.copy(scoreActual = 80)

        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(briefInitial) andThen flowOf(briefUpdated)

        viewModel = DailyScoreViewModel(repository, appEventBus, clock)
        viewModel.startEventSubscription(debounceMillis = 100L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(50, viewModel.uiState.value.currentScore)

        eventsFlow.emit(AppEvent.AttendanceMarked(1L, "PRESENT"))
        advanceTimeBy(150L)
        runCurrent()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(80, viewModel.uiState.value.currentScore)
        assertEquals(20, viewModel.uiState.value.remainingScore)
    }

    @Test
    fun debounce_collapsesMultipleEventsIntoSingleRefresh() = runTest {
        val brief = DailyBrief(
            date = "2026-08-01",
            jsonSnapshot = "{}",
            snapshotHash = "hash1",
            briefJson = "[]",
            scoreTarget = 100,
            scoreActual = 60
        )
        coEvery { repository.getBriefForDate("2026-08-01") } returns flowOf(brief)

        viewModel = DailyScoreViewModel(repository, appEventBus, clock)
        viewModel.startEventSubscription(debounceMillis = 500L)
        testDispatcher.scheduler.advanceUntilIdle()

        eventsFlow.emit(AppEvent.AssignmentStatusChanged(1L, "COMPLETED"))
        eventsFlow.emit(AppEvent.ProjectTaskCompleted(2L, 3L))
        eventsFlow.emit(AppEvent.DsaTopicUpdated(4L))

        advanceTimeBy(600L)
        runCurrent()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { repository.getBriefForDate("2026-08-01") }
    }
}
