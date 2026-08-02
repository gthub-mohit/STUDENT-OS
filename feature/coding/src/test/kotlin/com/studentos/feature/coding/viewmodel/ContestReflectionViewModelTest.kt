package com.studentos.feature.coding.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppResult
import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.CpProfile
import com.studentos.feature.coding.domain.model.CpReflection
import com.studentos.feature.coding.domain.repository.CpRepository
import com.studentos.feature.coding.domain.usecase.SaveContestReflectionUseCase
import com.studentos.feature.coding.presentation.viewmodel.ContestReflectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
class ContestReflectionViewModelTest {

    private class FakeAppEventBus : AppEventBus {
        val emittedEvents = mutableListOf<AppEvent>()
        private val _events = MutableSharedFlow<AppEvent>(replay = 10)
        override val events: SharedFlow<AppEvent> = _events.asSharedFlow()

        override suspend fun emit(event: AppEvent) {
            emittedEvents.add(event)
            _events.emit(event)
        }
    }

    private class FakeCpRepository : CpRepository {
        val reflectionMap = mutableMapOf<Long, CpReflection>()

        override fun getProfiles(): Flow<List<CpProfile>> = flowOf(emptyList())
        override fun getContests(profileId: Long): Flow<List<CpContest>> = flowOf(emptyList())
        override fun getAllContests(): Flow<List<CpContest>> = flowOf(emptyList())
        override fun getReflection(contestId: Long): Flow<CpReflection?> = MutableStateFlow(reflectionMap[contestId])

        override suspend fun saveReflection(reflection: CpReflection) {
            reflectionMap[reflection.contestId] = reflection
        }

        override suspend fun syncProfiles(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun syncProfile(platform: String, handle: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun addOrUpdateProfile(platform: String, handle: String): AppResult<Long> = AppResult.Success(1L)
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCpRepository
    private lateinit var eventBus: FakeAppEventBus
    private lateinit var saveUseCase: SaveContestReflectionUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCpRepository()
        eventBus = FakeAppEventBus()
        saveUseCase = SaveContestReflectionUseCase(repository, eventBus)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(contestId: Long = 10L): ContestReflectionViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("contestId" to contestId))
        return ContestReflectionViewModel(savedStateHandle, repository, saveUseCase)
    }

    @Test
    fun init_loadsExistingReflectionWhenPresent() = runTest {
        repository.reflectionMap[10L] = CpReflection(
            id = 5L,
            contestId = 10L,
            wentWrong = "Bad time management",
            toRevise = "Dynamic Programming",
            selfRating = 2
        )

        val viewModel = createViewModel(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Bad time management", state.wentWrong)
        assertEquals("Dynamic Programming", state.toRevise)
        assertEquals(2, state.selfRating)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun formEditing_detectsUnsavedChanges() = runTest {
        val viewModel = createViewModel(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onWentWrongChanged("Made calculation error")
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.onSelfRatingChanged(4)
        assertEquals(4, viewModel.uiState.value.selfRating)
    }

    @Test
    fun onSaveClicked_persistsDataAndEmitsEvent() = runTest {
        val viewModel = createViewModel(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onWentWrongChanged("Rushed solution")
        viewModel.onToReviseChanged("Greedy Algorithms")
        viewModel.onSelfRatingChanged(3)

        viewModel.onSaveClicked()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.hasUnsavedChanges)

        val saved = repository.reflectionMap[10L]
        assertEquals("Rushed solution", saved?.wentWrong)
        assertEquals("Greedy Algorithms", saved?.toRevise)
        assertEquals(3, saved?.selfRating)

        assertEquals(1, eventBus.emittedEvents.size)
        assertTrue(eventBus.emittedEvents[0] is AppEvent.ContestReflectionAdded)
    }

    @Test
    fun onBackClicked_withUnsavedChanges_promptsDiscardDialog() = runTest {
        val viewModel = createViewModel(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        var navBackCalled = false
        viewModel.onWentWrongChanged("Unsaved note")
        viewModel.onBackClicked { navBackCalled = true }

        assertTrue(viewModel.uiState.value.showDiscardDialog)
        assertFalse(navBackCalled)
    }

    @Test
    fun onConfirmDiscard_resetsDialogAndNavigatesBack() = runTest {
        val viewModel = createViewModel(10L)
        testDispatcher.scheduler.advanceUntilIdle()

        var navBackCalled = false
        viewModel.onWentWrongChanged("Unsaved note")
        viewModel.onBackClicked { }

        viewModel.onConfirmDiscard { navBackCalled = true }

        assertFalse(viewModel.uiState.value.showDiscardDialog)
        assertTrue(navBackCalled)
    }
}
