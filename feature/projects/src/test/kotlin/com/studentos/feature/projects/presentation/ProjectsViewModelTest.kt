package com.studentos.feature.projects.presentation

import app.cash.turbine.test
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.viewmodel.ProjectsViewModel
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private val repository: ProjectRepository = mockk()
    private lateinit var viewModel: ProjectsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun observeProjects_loadsActiveAndArchivedProjects() = runTest {
        val activeProject = ProjectDomain(
            id = 1L,
            title = "Student OS App",
            lastActivityAt = 1000L,
            totalTasks = 5,
            completedTasks = 2
        )
        val archivedProject = ProjectDomain(
            id = 2L,
            title = "Old Legacy App",
            archivedAt = 500L,
            lastActivityAt = 400L,
            totalTasks = 10,
            completedTasks = 10
        )

        coEvery { repository.getActiveProjects() } returns flowOf(listOf(activeProject))
        coEvery { repository.getArchivedProjects() } returns flowOf(listOf(archivedProject))

        viewModel = ProjectsViewModel(repository, clock)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.isEmpty)
            assertEquals(1, state.activeProjects.size)
            assertEquals("Student OS App", state.activeProjects[0].title)
            assertEquals(1, state.archivedProjects.size)
            assertEquals("Old Legacy App", state.archivedProjects[0].title)
        }
    }

    @Test
    fun createProject_invokesRepositoryCreateAndDismissesDialog() = runTest {
        coEvery { repository.getActiveProjects() } returns flowOf(emptyList())
        coEvery { repository.getArchivedProjects() } returns flowOf(emptyList())
        coEvery { repository.createProject("New System", 7) } returns 1L

        viewModel = ProjectsViewModel(repository, clock)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.openCreateDialog()
        assertTrue(viewModel.uiState.value.isCreateDialogOpen)

        viewModel.saveProject("New System", 7)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createProject("New System", 7) }
        assertFalse(viewModel.uiState.value.isCreateDialogOpen)
    }

    @Test
    fun archiveProject_invokesRepositoryArchive() = runTest {
        coEvery { repository.getActiveProjects() } returns flowOf(emptyList())
        coEvery { repository.getArchivedProjects() } returns flowOf(emptyList())
        coEvery { repository.archiveProject(1L) } returns Unit

        viewModel = ProjectsViewModel(repository, clock)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.archiveProject(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.archiveProject(1L) }
    }
}
