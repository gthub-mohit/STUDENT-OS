package com.studentos.feature.settings.presentation

import app.cash.turbine.test
import com.studentos.feature.settings.domain.model.SettingsDomain
import com.studentos.feature.settings.domain.repository.BackupRepository
import com.studentos.feature.settings.domain.repository.SettingsRepository
import com.studentos.feature.settings.presentation.viewmodel.SettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val backupRepository: BackupRepository = mockk(relaxed = true)
    private val settingsFlow = MutableStateFlow(SettingsDomain())

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { settingsRepository.observeAllSettings() } returns settingsFlow
        viewModel = SettingsViewModel(settingsRepository, backupRepository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsSettingsSuccessfully() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(75, state.settings.attendanceThreshold)
            assertEquals("07:00", state.settings.dailyBriefTimeHHmm)
            assertTrue(state.settings.aiEnabled)
            assertEquals("DEEPSEEK", state.settings.aiProvider)
        }
    }

    @Test
    fun updateAttendanceThreshold_callsRepository() = runTest {
        viewModel.updateAttendanceThreshold(85)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setAttendanceThreshold(85) }
    }

    @Test
    fun updateAiProvider_callsRepository() = runTest {
        viewModel.updateAiProvider("MOCK")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setAiProvider("MOCK") }
    }

    @Test
    fun updateDeepSeekApiKey_callsRepositoryAndSetsUserMessage() = runTest {
        viewModel.updateDeepSeekApiKey("sk-test-key-12345")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setDeepSeekApiKey("sk-test-key-12345") }
        assertEquals("API key saved successfully", viewModel.uiState.value.userMessage)
    }

    @Test
    fun updateCodeChefHandle_callsRepository() = runTest {
        viewModel.updateCodeChefHandle("tourist")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setCodeChefHandle("tourist") }
    }

    @Test
    fun updateDailyBriefTime_callsRepositoryAndClosesDialog() = runTest {
        viewModel.showTimePickerDialog()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showTimePickerDialog)

        viewModel.updateDailyBriefTime("08:30")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.setDailyBriefTimeHHmm("08:30") }
        assertFalse(viewModel.uiState.value.showTimePickerDialog)
    }

    @Test
    fun confirmReset_callsRepositoryReset() = runTest {
        viewModel.showResetConfirmation()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showResetConfirmDialog)

        viewModel.confirmReset()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.reset() }
        assertFalse(viewModel.uiState.value.showResetConfirmDialog)
        assertEquals("All settings reset to defaults", viewModel.uiState.value.userMessage)
    }

    @Test
    fun exportBackup_invokesCallbackWithJson() = runTest {
        coEvery { backupRepository.exportBackupJson() } returns "{\"version\":1}"

        var exportedText = ""
        viewModel.exportBackup { exportedText = it }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("{\"version\":1}", exportedText)
        assertEquals("Backup generated successfully", viewModel.uiState.value.userMessage)
    }

    @Test
    fun importBackup_validatesAndRestoresSuccessfully() = runTest {
        coEvery { backupRepository.restoreBackupJson("{\"version\":1}") } returns Result.success(Unit)

        viewModel.prepareImportBackup("{\"version\":1}")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showImportConfirmDialog)

        var successCallbackCalled = false
        viewModel.confirmImport { successCallbackCalled = true }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { backupRepository.restoreBackupJson("{\"version\":1}") }
        assertTrue(successCallbackCalled)
        assertFalse(viewModel.uiState.value.showImportConfirmDialog)
        assertEquals("Backup restored successfully", viewModel.uiState.value.userMessage)
    }
}
