package com.studentos.feature.projects.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.studentos.feature.projects.domain.model.BugDomain
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.state.BugSeverityFilter
import com.studentos.feature.projects.presentation.state.BugStatusFilter
import com.studentos.feature.projects.presentation.viewmodel.BugTrackerViewModel
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
class BugTrackerViewModelTest {

    private val repository: ProjectRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val sampleProject = ProjectDomain(
        id = 1L,
        title = "Student OS App",
        lastActivityAt = 1000L
    )

    private val sampleBug1 = BugDomain(
        id = 200L,
        projectId = 1L,
        description = "Null pointer in navigation",
        severity = BugDomain.SEVERITY_HIGH,
        status = BugDomain.STATUS_OPEN
    )

    private val sampleBug2 = BugDomain(
        id = 201L,
        projectId = 1L,
        description = "UI text truncation on small screens",
        severity = BugDomain.SEVERITY_LOW,
        status = BugDomain.STATUS_RESOLVED
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getProjectById(1L) } returns flowOf(sampleProject)
        every { repository.getBugsForProject(1L) } returns flowOf(listOf(sampleBug1, sampleBug2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsProjectAndBugsSuccessfully() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = BugTrackerViewModel(repository, savedStateHandle)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("Student OS App", state.project?.title)
            assertEquals(2, state.bugs.size)
            assertEquals(1, state.openCount)
            assertEquals(1, state.resolvedCount)
            assertEquals(1, state.filteredBugs.size) // Default filter OPEN
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setStatusFilter_filtersBugsCorrectly() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = BugTrackerViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setStatusFilter(BugStatusFilter.ALL)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(2, state.filteredBugs.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveBug_createsNewBugWhenEditIsNull() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = BugTrackerViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveBug("Database crash on migration", BugDomain.SEVERITY_HIGH)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createBug(1L, "Database crash on migration", BugDomain.SEVERITY_HIGH) }
    }

    @Test
    fun toggleBugResolution_resolvesOpenBug() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = BugTrackerViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleBugResolution(sampleBug1)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.resolveBug(200L) }
    }

    @Test
    fun deleteBug_callsRepositoryDeleteBug() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = BugTrackerViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteBug(200L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteBug(200L) }
    }
}
