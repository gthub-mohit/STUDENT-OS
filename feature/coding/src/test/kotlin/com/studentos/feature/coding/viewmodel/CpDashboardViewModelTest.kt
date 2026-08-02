package com.studentos.feature.coding.viewmodel

import app.cash.turbine.test
import com.studentos.core.events.AppResult
import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.CpProfile
import com.studentos.feature.coding.domain.model.CpReflection
import com.studentos.feature.coding.domain.repository.CpRepository
import com.studentos.feature.coding.domain.usecase.GetGroupedContestsUseCase
import com.studentos.feature.coding.presentation.viewmodel.CpDashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CpDashboardViewModelTest {

    private class FakeCpRepository : CpRepository {
        val profilesFlow = MutableStateFlow<List<CpProfile>>(emptyList())
        val contestsFlow = MutableStateFlow<List<CpContest>>(emptyList())
        var syncCalled = false

        override fun getProfiles(): Flow<List<CpProfile>> = profilesFlow
        override fun getContests(profileId: Long): Flow<List<CpContest>> = contestsFlow
        override fun getAllContests(): Flow<List<CpContest>> = contestsFlow
        override fun getReflection(contestId: Long): Flow<CpReflection?> = flowOf(null)
        override suspend fun saveReflection(reflection: CpReflection) {}

        override suspend fun syncProfiles(): AppResult<Unit> {
            syncCalled = true
            return AppResult.Success(Unit)
        }

        override suspend fun syncProfile(platform: String, handle: String): AppResult<Unit> {
            syncCalled = true
            return AppResult.Success(Unit)
        }

        override suspend fun addOrUpdateProfile(platform: String, handle: String): AppResult<Long> {
            syncCalled = true
            val current = profilesFlow.value.toMutableList()
            current.add(CpProfile(id = 1L, platform = platform, handle = handle))
            profilesFlow.value = current
            return AppResult.Success(1L)
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCpRepository
    private lateinit var viewModel: CpDashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeCpRepository()
        val getGroupedContestsUseCase = GetGroupedContestsUseCase(repository)
        viewModel = CpDashboardViewModel(repository, getGroupedContestsUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_isInitialLoadingState() {
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
    }

    @Test
    fun uiState_emptyProfiles_emitsEmptyState() = runTest {
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            testDispatcher.scheduler.advanceUntilIdle()

            val emptyState = awaitItem()
            assertFalse(emptyState.isLoading)
            assertTrue(emptyState.isEmpty)
            assertTrue(emptyState.profiles.isEmpty())
            assertTrue(emptyState.contests.isEmpty())
            assertNull(emptyState.lastSyncedAt)
        }
    }

    @Test
    fun uiState_populatedProfilesAndContests_emitsMappedStateAndLastSyncedTimestamp() = runTest {
        val syncTime = 1700000000000L
        val profiles = listOf(
            CpProfile(id = 1L, platform = "CODECHEF", handle = "chef123", currentRating = 1800, lastSyncedAt = syncTime)
        )
        val contests = listOf(
            CpContest(id = 10L, profileId = 1L, contestName = "Starters 100", contestDate = syncTime - 1000L, rank = 5, ratingChange = 50, problemsSolved = 4)
        )

        viewModel.uiState.test {
            awaitItem() // Initial loading state

            repository.profilesFlow.value = profiles
            repository.contestsFlow.value = contests
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.isEmpty)
            assertEquals(1, state.profiles.size)
            assertEquals("chef123", state.profiles[0].handle)
            assertEquals(1, state.contests.size)
            assertEquals(syncTime, state.lastSyncedAt)
        }
    }

    @Test
    fun triggerSync_invokesRepositorySync() = runTest {
        viewModel.triggerSync()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(repository.syncCalled)
    }
}
