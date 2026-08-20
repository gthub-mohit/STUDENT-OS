package com.studentos.feature.projects.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.domain.model.ProjectTaskPriority
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.state.ProjectTaskStatusFilter
import com.studentos.feature.projects.presentation.viewmodel.ProjectTaskViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectTaskViewModelTest {

    private val repository: ProjectRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val sampleProject = ProjectDomain(
        id = 1L,
        title = "OS Kernel",
        lastActivityAt = 1000L
    )

    private val sampleTask1 = ProjectTaskDomain(
        id = 10L,
        projectId = 1L,
        title = "Memory Manager",
        isNextAction = true,
        isParallel = false,
        completedAt = null,
        sortOrder = 0,
        priority = ProjectTaskPriority.HIGH
    )

    private val sampleTask2 = ProjectTaskDomain(
        id = 11L,
        projectId = 1L,
        title = "Scheduler",
        dependencyTaskId = 10L,
        isNextAction = false,
        isParallel = false,
        completedAt = null,
        sortOrder = 1,
        priority = ProjectTaskPriority.MEDIUM
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getProjectById(1L) } returns flowOf(sampleProject)
        every { repository.getTasksForProject(1L) } returns flowOf(listOf(sampleTask1, sampleTask2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsProjectAndTasksSuccessfully() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("OS Kernel", state.project?.title)
            assertEquals(2, state.tasks.size)
            assertEquals(1, state.availableTasks.size)
            assertEquals("Memory Manager", state.availableTasks[0].title)
            assertEquals(1, state.blockedTasks.size)
            assertEquals("Scheduler", state.blockedTasks[0].title)
            assertEquals("Memory Manager", state.nextAction.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveTask_createsNewTaskWithDependencyAndPriority() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveTask(
            title = "Write Bootloader",
            dependencyTaskId = 10L,
            priority = ProjectTaskPriority.HIGH,
            deadline = 5000L
        )
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            repository.createTask(
                projectId = 1L,
                title = "Write Bootloader",
                isParallel = false,
                dependencyTaskId = 10L,
                priority = ProjectTaskPriority.HIGH,
                deadline = 5000L
            )
        }
    }

    @Test
    fun saveTask_rejectsCircularDependency() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // Open edit dialog for Task 10 (which is dependency of Task 11)
        viewModel.openEditTaskDialog(sampleTask1)

        // Attempt to make Task 10 depend on Task 11 -> creates cycle (10 -> 11 -> 10)
        viewModel.saveTask(
            title = "Memory Manager",
            dependencyTaskId = 11L,
            priority = ProjectTaskPriority.HIGH,
            deadline = null
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("This dependency would create a circular workflow.", viewModel.uiState.value.dialogError)
    }

    @Test
    fun toggleTaskCompletion_completesAvailableTask() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleTaskCompletion(sampleTask1)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.completeTask(10L) }
    }

    @Test
    fun toggleTaskCompletion_preventsCompletingBlockedTask() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        // sampleTask2 is blocked by sampleTask1
        viewModel.toggleTaskCompletion(sampleTask2)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Waiting for 'Memory Manager'"))
    }

    @Test
    fun filterStatus_updatesFilteredTasks() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setStatusFilter(ProjectTaskStatusFilter.AVAILABLE)
        assertEquals(1, viewModel.uiState.value.filteredTasks.size)
        assertEquals("Memory Manager", viewModel.uiState.value.filteredTasks[0].title)
        assertEquals("Filters (1)", viewModel.uiState.value.filterButtonLabel)

        viewModel.clearFilters()
        assertEquals(2, viewModel.uiState.value.filteredTasks.size)
        assertEquals("Filters", viewModel.uiState.value.filterButtonLabel)
    }
}
