package com.studentos.feature.intelligence.presentation

import com.studentos.core.database.dao.ActivityLogDao
import com.studentos.core.database.dao.GoalDao
import com.studentos.core.database.entity.ActivityLogEntity
import com.studentos.core.database.entity.GoalEntity
import com.studentos.feature.intelligence.domain.usecase.CalculateGoalProgressUseCase
import com.studentos.feature.intelligence.domain.usecase.ManageGoalsUseCase
import com.studentos.feature.intelligence.domain.usecase.SaveGoalUseCase
import com.studentos.feature.intelligence.presentation.state.GoalDialogState
import com.studentos.feature.intelligence.presentation.state.GoalsTab
import com.studentos.feature.intelligence.presentation.viewmodel.TrajectoryGoalsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrajectoryGoalsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val goalDao: GoalDao = mockk()
    private val activityLogDao: ActivityLogDao = mockk()
    private val calculateGoalProgressUseCase = CalculateGoalProgressUseCase()
    private val saveGoalUseCase: SaveGoalUseCase = mockk()
    private val manageGoalsUseCase: ManageGoalsUseCase = mockk()

    private lateinit var viewModel: TrajectoryGoalsViewModel

    private val sampleActiveGoal = GoalEntity(
        id = 1L,
        title = "Solve 200 DSA Questions",
        category = "DSA",
        metricType = "COUNT",
        unit = "questions",
        startValue = 0.0,
        targetValue = 200.0,
        startDate = 1000L,
        targetDate = 10000L,
        isArchived = false,
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private val sampleArchivedGoal = GoalEntity(
        id = 2L,
        title = "Old Project Goal",
        category = "PROJECT",
        metricType = "HOURS",
        unit = "hours",
        startValue = 0.0,
        targetValue = 50.0,
        startDate = 500L,
        targetDate = 2000L,
        isArchived = true,
        createdAt = 500L,
        updatedAt = 2500L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { goalDao.observeActiveGoals() } returns flowOf(listOf(sampleActiveGoal))
        every { goalDao.observeArchivedGoals() } returns flowOf(listOf(sampleArchivedGoal))
        every { activityLogDao.observeAllLogs() } returns flowOf(
            listOf(
                ActivityLogEntity(
                    id = 1,
                    goalId = 1L,
                    timestamp = 2000L,
                    metricDelta = 25.0,
                    activityType = "DSA",
                    summary = "Trees practice",
                    confirmedAt = 2000L
                )
            )
        )

        viewModel = TrajectoryGoalsViewModel(
            goalDao = goalDao,
            activityLogDao = activityLogDao,
            calculateGoalProgressUseCase = calculateGoalProgressUseCase,
            saveGoalUseCase = saveGoalUseCase,
            manageGoalsUseCase = manageGoalsUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state observes active and archived goals and computes progress reactively`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.activeGoals.size)
        assertEquals(1, state.archivedGoals.size)

        val active = state.activeGoals[0]
        assertEquals("Solve 200 DSA Questions", active.goal.title)
        assertEquals(25.0, active.actualProgress, 0.001)
        assertEquals(1, active.totalLogsCount)
    }

    @Test
    fun `selectTab switches between active and archived goals`() {
        assertEquals(GoalsTab.ACTIVE, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(GoalsTab.ARCHIVED)
        assertEquals(GoalsTab.ARCHIVED, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(GoalsTab.ACTIVE)
        assertEquals(GoalsTab.ACTIVE, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `toggleGraphExpanded toggles goalId in expanded set`() {
        assertEquals(0, viewModel.uiState.value.expandedGraphGoalIds.size)

        viewModel.toggleGraphExpanded(1L)
        assertTrue(viewModel.uiState.value.expandedGraphGoalIds.contains(1L))

        viewModel.toggleGraphExpanded(1L)
        assertEquals(0, viewModel.uiState.value.expandedGraphGoalIds.size)
    }

    @Test
    fun `openCreateGoalDialog sets dialog state to Create`() {
        viewModel.openCreateGoalDialog()
        assertEquals(GoalDialogState.Create, viewModel.uiState.value.dialogState)

        viewModel.dismissDialog()
        assertNull(viewModel.uiState.value.dialogState)
    }

    @Test
    fun `openEditGoalDialog sets dialog state to Edit with goal data`() {
        viewModel.openEditGoalDialog(sampleActiveGoal)
        val state = viewModel.uiState.value.dialogState
        assertTrue(state is GoalDialogState.Edit)
        assertEquals(1L, (state as GoalDialogState.Edit).goal.id)
    }

    @Test
    fun `toggleArchive calls manageGoalsUseCase and updates user message`() = runTest {
        coEvery { manageGoalsUseCase.setArchived(1L, true) } returns Result.success(Unit)

        viewModel.toggleArchive(sampleActiveGoal)
        advanceUntilIdle()

        coVerify(exactly = 1) { manageGoalsUseCase.setArchived(1L, true) }
        assertEquals("Goal moved to archive.", viewModel.uiState.value.userMessage)
    }

    @Test
    fun `delete confirmation flow prompts and executes deletion`() = runTest {
        coEvery { manageGoalsUseCase.deleteGoal(1L) } returns Result.success(Unit)

        viewModel.promptDelete(sampleActiveGoal)
        assertNotNull(viewModel.uiState.value.deleteConfirmGoal)

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deleteConfirmGoal)
        coVerify(exactly = 1) { manageGoalsUseCase.deleteGoal(1L) }
        assertTrue(viewModel.uiState.value.userMessage?.contains("deleted") == true)
    }
}
