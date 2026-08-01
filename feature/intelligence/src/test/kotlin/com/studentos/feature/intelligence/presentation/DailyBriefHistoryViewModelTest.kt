package com.studentos.feature.intelligence.presentation

import app.cash.turbine.test
import com.studentos.feature.intelligence.domain.model.DailyBrief
import com.studentos.feature.intelligence.domain.model.DailyBriefSummaryDomain
import com.studentos.feature.intelligence.domain.usecase.GetBriefHistoryUseCase
import com.studentos.feature.intelligence.presentation.viewmodel.DailyBriefHistoryViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DailyBriefHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getBriefHistoryUseCase: GetBriefHistoryUseCase = mockk()

    private lateinit var viewModel: DailyBriefHistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsSortedHistory_newestFirst() = runTest {
        val summaries = listOf(
            DailyBriefSummaryDomain(1, "2026-07-28", 100, 80, DailyBrief.GUIDANCE_SOURCE_DETERMINISTIC, 1000L),
            DailyBriefSummaryDomain(2, "2026-08-01", 100, 95, DailyBrief.GUIDANCE_SOURCE_LLM, 2000L)
        )

        coEvery { getBriefHistoryUseCase() } returns flowOf(summaries)

        viewModel = DailyBriefHistoryViewModel(getBriefHistoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.isEmpty)
            assertEquals(2, state.history.size)
            assertEquals("2026-08-01", state.history[0].date)
            assertEquals("2026-07-28", state.history[1].date)
        }
    }

    @Test
    fun emptyHistory_setsIsEmptyTrue() = runTest {
        coEvery { getBriefHistoryUseCase() } returns flowOf(emptyList())

        viewModel = DailyBriefHistoryViewModel(getBriefHistoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            assertTrue(state.history.isEmpty())
        }
    }
}
