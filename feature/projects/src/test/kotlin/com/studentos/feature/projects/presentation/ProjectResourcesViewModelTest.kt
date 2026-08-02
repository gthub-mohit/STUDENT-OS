package com.studentos.feature.projects.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectResourceDomain
import com.studentos.feature.projects.domain.repository.ProjectRepository
import com.studentos.feature.projects.presentation.viewmodel.ProjectResourcesViewModel
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
class ProjectResourcesViewModelTest {

    private val repository: ProjectRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val sampleProject = ProjectDomain(
        id = 1L,
        title = "Cloud Infrastructure",
        lastActivityAt = 1000L
    )

    private val sampleResource1 = ProjectResourceDomain(
        id = 300L,
        projectId = 1L,
        url = "https://github.com/studentos/cloud",
        label = "GitHub Repository",
        type = ProjectResourceDomain.TYPE_LINK
    )

    private val sampleResource2 = ProjectResourceDomain(
        id = 301L,
        projectId = 1L,
        url = "docs/architecture.md",
        label = "Architecture Specs",
        type = ProjectResourceDomain.TYPE_DOCUMENTATION
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getProjectById(1L) } returns flowOf(sampleProject)
        every { repository.getResourcesForProject(1L) } returns flowOf(listOf(sampleResource1, sampleResource2))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsProjectAndResourcesSuccessfully() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectResourcesViewModel(repository, savedStateHandle)

        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertEquals("Cloud Infrastructure", state.project?.title)
            assertEquals(2, state.resources.size)
            assertEquals("GitHub Repository", state.resources[0].label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveResource_createsNewResourceWhenEditIsNull() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectResourcesViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveResource("https://aws.amazon.com/docs", "AWS Docs", ProjectResourceDomain.TYPE_DOCUMENTATION)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createResource(1L, "https://aws.amazon.com/docs", "AWS Docs", ProjectResourceDomain.TYPE_DOCUMENTATION) }
    }

    @Test
    fun deleteResource_callsRepositoryDeleteResource() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("projectId" to "1"))
        val viewModel = ProjectResourcesViewModel(repository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteResource(300L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.deleteResource(300L) }
    }
}
