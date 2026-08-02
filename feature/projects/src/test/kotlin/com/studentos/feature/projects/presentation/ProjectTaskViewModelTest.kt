package com.studentos.feature.projects.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
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
        sortOrder = 0
    )

    private val sampleTask2 = ProjectTaskDomain(
        id = 11L,
        projectId = 1L,
        title = "Scheduler",
        isNextAction = false,
        isParallel = false,
        completedAt = null,
        sortOrder = 1
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
            assertEquals("Memory Manager", state.activeNextAction?.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveTask_createsNewTaskWhenEditTaskIsNull() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveTask("Write Bootloader")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createTask(1L, "Write Bootloader", false) }
    }

    @Test
    fun toggleTaskCompletion_completesUnfinishedTask() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleTaskCompletion(sampleTask1)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.completeTask(10L) }
    }

    @Test
    fun setNextAction_callsRepositorySetNextAction() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setNextAction(11L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.setNextAction(1L, 11L) }
    }

    @Test
    fun toggleParallelMode_updatesParallelModeInRepository() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectTaskViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleParallelMode(true)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.toggleParallelMode(1L, true) }
    }
}
