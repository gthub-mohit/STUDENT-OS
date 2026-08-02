package com.studentos.feature.projects.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.studentos.feature.projects.domain.model.MilestoneDomain
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.viewmodel.MilestoneViewModel
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MilestoneViewModelTest {

    private val repository: ProjectRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val sampleProject = ProjectDomain(
        id = 1L,
        title = "AI Engine",
        lastActivityAt = 1000L
    )

    private val sampleMilestone1 = MilestoneDomain(
        id = 100L,
        projectId = 1L,
        title = "v1.0 Release",
        description = "Initial MVP release",
        targetDate = 2000L,
        status = MilestoneDomain.STATUS_PENDING
    )

    private val sampleMilestone2 = MilestoneDomain(
        id = 101L,
        projectId = 1L,
        title = "Alpha Testing",
        status = MilestoneDomain.STATUS_DONE
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getProjectById(1L) } returns flowOf(sampleProject)
        every { repository.getMilestonesForProject(1L) } returns flowOf(listOf(sampleMilestone1, sampleMilestone2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsProjectAndMilestonesSuccessfully() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = MilestoneViewModel(repository, savedStateHandle)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("AI Engine", state.project?.title)
            assertEquals(2, state.milestones.size)
            assertEquals(1, state.completedCount)
            assertEquals(50f, state.progressPercentage, 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveMilestone_createsNewMilestoneWhenEditIsNull() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = MilestoneViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveMilestone("Beta Launch", "Full feature test", 5000L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createMilestone(1L, "Beta Launch", "Full feature test", 5000L) }
    }

    @Test
    fun toggleMilestoneCompletion_completesPendingMilestone() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = MilestoneViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleMilestoneCompletion(sampleMilestone1)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.completeMilestone(100L) }
    }

    @Test
    fun deleteMilestone_callsRepositoryDeleteMilestone() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = MilestoneViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteMilestone(100L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteMilestone(100L) }
    }
}
