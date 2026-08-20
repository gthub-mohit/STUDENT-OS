package com.studentos.feature.settings.presentation

import app.cash.turbine.test
import com.studentos.core.database.dao.AiCallLogDao
import com.studentos.core.database.entity.AiCallLogEntity
import com.studentos.feature.settings.domain.model.SettingsDomain
import com.studentos.feature.settings.domain.repository.SettingsRepository
import com.studentos.feature.settings.presentation.viewmodel.AiDiagnosticsViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiDiagnosticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val aiCallLogDao: AiCallLogDao = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private val logsFlow = MutableStateFlow<List<AiCallLogEntity>>(emptyList())
    private val settingsFlow = MutableStateFlow(SettingsDomain(aiMaxCallsPerDay = 15, aiProvider = "DEEPSEEK"))

    private lateinit var viewModel: AiDiagnosticsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { aiCallLogDao.getRecentLogs(50) } returns logsFlow
        coEvery { settingsRepository.observeAllSettings() } returns settingsFlow
        viewModel = AiDiagnosticsViewModel(aiCallLogDao, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_computesStatsCorrectly() = runTest {
        val now = System.currentTimeMillis()
        val sampleLogs = listOf(
            AiCallLogEntity(
                id = 1L,
                triggeredBy = "MORNING_GENERATION",
                snapshotHash = "hash1",
                tokenCount = 500,
                latencyMs = 1200L,
                wasCacheHit = false,
                wasDelta = false,
                success = true,
                createdAt = now
            ),
            AiCallLogEntity(
                id = 2L,
                triggeredBy = "AttendanceMarked",
                snapshotHash = "hash2",
                tokenCount = 150,
                latencyMs = 300L,
                wasCacheHit = true,
                wasDelta = true,
                success = true,
                createdAt = now
            ),
            AiCallLogEntity(
                id = 3L,
                triggeredBy = "AssignmentStatusChanged",
                snapshotHash = "hash3",
                tokenCount = 0,
                latencyMs = 5000L,
                wasCacheHit = false,
                wasDelta = false,
                success = false,
                errorMessage = "HTTP 503",
                createdAt = now
            )
        )
        logsFlow.value = sampleLogs

        viewModel.uiState.test {
            val state = awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            val loaded = if (state.isLoading) awaitItem() else state

            assertFalse(loaded.isLoading)
            assertEquals(3, loaded.callsToday)
            assertEquals(15, loaded.maxCallsPerDay)
            assertEquals(650, loaded.tokensToday)
            assertEquals("DEEPSEEK", loaded.aiProvider)
            assertEquals(3, loaded.logs.size)
            // Success rate: 2/3 = 66.66%
            assertEquals(66.66f, loaded.successRatePercent, 0.1f)
        }
    }
}
