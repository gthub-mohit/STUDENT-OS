package com.studentos.feature.projects.domain

import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.presentation.state.ProjectTaskUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectDomainTest {

    @Test
    fun progressPercentage_calculatesAccuratelyAcrossAllEdgeCases() {
        // 0/0 tasks
        val project0of0 = ProjectDomain(
            id = 1L,
            title = "Empty",
            lastActivityAt = 1000L,
            totalTasks = 0,
            completedTasks = 0
        )
        assertEquals(0f, project0of0.progressPercentage, 0.001f)

        // 0/4 tasks
        val project0of4 = ProjectDomain(
            id = 2L,
            title = "0 of 4",
            lastActivityAt = 1000L,
            totalTasks = 4,
            completedTasks = 0
        )
        assertEquals(0f, project0of4.progressPercentage, 0.001f)

        // 1/4 tasks
        val project1of4 = ProjectDomain(
            id = 3L,
            title = "1 of 4",
            lastActivityAt = 1000L,
            totalTasks = 4,
            completedTasks = 1
        )
        assertEquals(25f, project1of4.progressPercentage, 0.001f)

        // 2/4 tasks
        val project2of4 = ProjectDomain(
            id = 4L,
            title = "2 of 4",
            lastActivityAt = 1000L,
            totalTasks = 4,
            completedTasks = 2
        )
        assertEquals(50f, project2of4.progressPercentage, 0.001f)

        // 3/4 tasks
        val project3of4 = ProjectDomain(
            id = 5L,
            title = "3 of 4",
            lastActivityAt = 1000L,
            totalTasks = 4,
            completedTasks = 3
        )
        assertEquals(75f, project3of4.progressPercentage, 0.001f)

        // 4/4 tasks
        val project4of4 = ProjectDomain(
            id = 6L,
            title = "4 of 4",
            lastActivityAt = 1000L,
            totalTasks = 4,
            completedTasks = 4
        )
        assertEquals(100f, project4of4.progressPercentage, 0.001f)
    }

    @Test
    fun isInactive_checksThresholdCorrectly() {
        val now = 10_000_000_000L
        val oneDayMs = 1000L * 60 * 60 * 24

        val recentProject = ProjectDomain(
            id = 1L,
            title = "Active",
            lastActivityAt = now - (2 * oneDayMs),
            inactivityThresholdDays = 7
        )
        assertFalse(recentProject.isInactive(now))

        val inactiveProject = ProjectDomain(
            id = 2L,
            title = "Inactive",
            lastActivityAt = now - (8 * oneDayMs),
            inactivityThresholdDays = 7
        )
        assertTrue(inactiveProject.isInactive(now))

        // Archived projects are never marked inactive
        val archivedProject = ProjectDomain(
            id = 3L,
            title = "Archived",
            archivedAt = now - (10 * oneDayMs),
            lastActivityAt = now - (10 * oneDayMs),
            inactivityThresholdDays = 7
        )
        assertFalse(archivedProject.isInactive(now))
    }

    @Test
    fun projectTaskUiState_separatesPendingAndCompletedCorrectly() {
        val task1 = ProjectTaskDomain(1L, 1L, "T1", isNextAction = true, isParallel = false, completedAt = null, sortOrder = 0)
        val task2 = ProjectTaskDomain(2L, 1L, "T2", isNextAction = false, isParallel = false, completedAt = null, sortOrder = 1)
        val task3 = ProjectTaskDomain(3L, 1L, "T3", isNextAction = false, isParallel = false, completedAt = 1000L, sortOrder = 2)

        val state = ProjectTaskUiState(
            isLoading = false,
            tasks = listOf(task1, task2, task3),
            isParallelMode = false
        )

        assertEquals(2, state.pendingTasks.size)
        assertEquals(1, state.completedTasks.size)
        assertEquals("T1", state.activeNextAction?.title)
    }

    @Test
    fun projectTaskUiState_emptyHandling() {
        val emptyState = ProjectTaskUiState(isLoading = false, tasks = emptyList())
        assertTrue(emptyState.isEmpty)
        assertNull(emptyState.activeNextAction)
    }
}
