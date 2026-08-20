package com.studentos.feature.projects.domain

import com.studentos.feature.projects.domain.model.NextActionStatus
import com.studentos.feature.projects.domain.model.ProjectDomain
import com.studentos.feature.projects.domain.model.ProjectTaskDomain
import com.studentos.feature.projects.domain.model.ProjectTaskEngine
import com.studentos.feature.projects.domain.model.ProjectTaskPriority
import com.studentos.feature.projects.domain.model.ProjectTaskState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTaskEngineTest {

    private val nowMs = 1_700_000_000_000L // arbitrary fixed timestamp
    private val oneDayMs = 24 * 60 * 60 * 1000L

    // 1. Task with no dependency -> AVAILABLE
    @Test
    fun test1_taskWithNoDependency_isAvailable() {
        val task = ProjectTaskDomain(id = 1L, projectId = 1L, title = "Independent Task", dependencyTaskId = null)
        val state = ProjectTaskEngine.getTaskState(task, mapOf(1L to task))
        assertEquals(ProjectTaskState.AVAILABLE, state)
    }

    // 2. Task with incomplete dependency -> BLOCKED
    @Test
    fun test2_taskWithIncompleteDependency_isBlocked() {
        val prerequisite = ProjectTaskDomain(id = 1L, projectId = 1L, title = "Design Circuit", completedAt = null)
        val dependent = ProjectTaskDomain(id = 2L, projectId = 1L, title = "Assemble Circuit", dependencyTaskId = 1L, completedAt = null)
        val map = listOf(prerequisite, dependent).associateBy { it.id }

        val state = ProjectTaskEngine.getTaskState(dependent, map)
        assertEquals(ProjectTaskState.BLOCKED, state)

        val blocker = ProjectTaskEngine.getBlockerTask(dependent, map)
        assertEquals("Design Circuit", blocker?.title)
    }

    // 3. Task with completed dependency -> AVAILABLE
    @Test
    fun test3_taskWithCompletedDependency_isAvailable() {
        val prerequisite = ProjectTaskDomain(id = 1L, projectId = 1L, title = "Design Circuit", completedAt = nowMs - 1000)
        val dependent = ProjectTaskDomain(id = 2L, projectId = 1L, title = "Assemble Circuit", dependencyTaskId = 1L, completedAt = null)
        val map = listOf(prerequisite, dependent).associateBy { it.id }

        val state = ProjectTaskEngine.getTaskState(dependent, map)
        assertEquals(ProjectTaskState.AVAILABLE, state)
    }

    // 4. Completed task -> COMPLETED
    @Test
    fun test4_completedTask_isCompleted() {
        val completedTask = ProjectTaskDomain(id = 1L, projectId = 1L, title = "Done Task", completedAt = nowMs)
        val state = ProjectTaskEngine.getTaskState(completedTask, mapOf(1L to completedTask))
        assertEquals(ProjectTaskState.COMPLETED, state)
    }

    // 5. Dependency chain resolution (A -> B -> C)
    @Test
    fun test5_dependencyChain_resolvesStepByStep() {
        val taskA = ProjectTaskDomain(id = 1L, projectId = 1L, title = "A", completedAt = null)
        val taskB = ProjectTaskDomain(id = 2L, projectId = 1L, title = "B", dependencyTaskId = 1L, completedAt = null)
        val taskC = ProjectTaskDomain(id = 3L, projectId = 1L, title = "C", dependencyTaskId = 2L, completedAt = null)

        val map1 = listOf(taskA, taskB, taskC).associateBy { it.id }
        assertEquals(ProjectTaskState.AVAILABLE, ProjectTaskEngine.getTaskState(taskA, map1))
        assertEquals(ProjectTaskState.BLOCKED, ProjectTaskEngine.getTaskState(taskB, map1))
        assertEquals(ProjectTaskState.BLOCKED, ProjectTaskEngine.getTaskState(taskC, map1))

        // Complete A: unlocks B, C remains blocked
        val taskACompleted = taskA.copy(completedAt = nowMs)
        val map2 = listOf(taskACompleted, taskB, taskC).associateBy { it.id }
        assertEquals(ProjectTaskState.COMPLETED, ProjectTaskEngine.getTaskState(taskACompleted, map2))
        assertEquals(ProjectTaskState.AVAILABLE, ProjectTaskEngine.getTaskState(taskB, map2))
        assertEquals(ProjectTaskState.BLOCKED, ProjectTaskEngine.getTaskState(taskC, map2))

        // Complete B: unlocks C
        val taskBCompleted = taskB.copy(completedAt = nowMs)
        val map3 = listOf(taskACompleted, taskBCompleted, taskC).associateBy { it.id }
        assertEquals(ProjectTaskState.AVAILABLE, ProjectTaskEngine.getTaskState(taskC, map3))
    }

    // 6. Self-dependency rejected
    @Test
    fun test6_selfDependency_rejected() {
        val task1 = ProjectTaskDomain(id = 10L, projectId = 1L, title = "Task 1")
        val tasks = listOf(task1)

        val createsCycle = ProjectTaskEngine.wouldCreateCycle(
            taskId = 10L,
            targetDependencyId = 10L,
            existingTasks = tasks
        )
        assertTrue(createsCycle)
    }

    // 7. Circular dependency rejected
    @Test
    fun test7_circularDependency_rejected() {
        // A -> B -> C. If A wants to depend on C, it forms a cycle: A -> C -> B -> A
        val taskA = ProjectTaskDomain(id = 1L, projectId = 1L, title = "A", dependencyTaskId = null)
        val taskB = ProjectTaskDomain(id = 2L, projectId = 1L, title = "B", dependencyTaskId = 1L)
        val taskC = ProjectTaskDomain(id = 3L, projectId = 1L, title = "C", dependencyTaskId = 2L)
        val tasks = listOf(taskA, taskB, taskC)

        // Setting A to depend on C creates a cycle
        val cycle = ProjectTaskEngine.wouldCreateCycle(
            taskId = 1L,
            targetDependencyId = 3L,
            existingTasks = tasks
        )
        assertTrue(cycle)

        // Independent task D depending on C does NOT create a cycle
        val noCycle = ProjectTaskEngine.wouldCreateCycle(
            taskId = 4L,
            targetDependencyId = 3L,
            existingTasks = tasks
        )
        assertFalse(noCycle)
    }

    // 8. Completing dependency unlocks dependent task
    @Test
    fun test8_completingDependency_unlocksDependentTask() {
        val task1 = ProjectTaskDomain(id = 1L, projectId = 1L, title = "Design", completedAt = null)
        val task2 = ProjectTaskDomain(id = 2L, projectId = 1L, title = "Build", dependencyTaskId = 1L, completedAt = null)

        val beforeMap = listOf(task1, task2).associateBy { it.id }
        assertEquals(ProjectTaskState.BLOCKED, ProjectTaskEngine.getTaskState(task2, beforeMap))

        val afterMap = listOf(task1.copy(completedAt = nowMs), task2).associateBy { it.id }
        assertEquals(ProjectTaskState.AVAILABLE, ProjectTaskEngine.getTaskState(task2, afterMap))
    }

    // 9. Blocked tasks are never selected as Next Action
    @Test
    fun test9_blockedTasks_areNeverSelectedAsNextAction() {
        val prerequisite = ProjectTaskDomain(id = 1L, projectId = 1L, title = "Prerequisite", completedAt = null)
        val highPriorityBlocked = ProjectTaskDomain(
            id = 2L,
            projectId = 1L,
            title = "Blocked High Priority",
            dependencyTaskId = 1L,
            priority = ProjectTaskPriority.HIGH,
            deadline = nowMs - oneDayMs // Overdue
        )

        val rec = ProjectTaskEngine.computeNextAction(listOf(prerequisite, highPriorityBlocked), nowMs)
        assertEquals(NextActionStatus.AVAILABLE_TASK, rec.status)
        assertEquals("Prerequisite", rec.task?.title)
    }

    // 10. Overdue available task gets higher Next Action priority
    @Test
    fun test10_overdueAvailableTask_getsHigherNextActionPriority() {
        val normalTask = ProjectTaskDomain(
            id = 1L,
            projectId = 1L,
            title = "Normal Task",
            deadline = nowMs + 5 * oneDayMs,
            priority = ProjectTaskPriority.MEDIUM
        )
        val overdueTask = ProjectTaskDomain(
            id = 2L,
            projectId = 1L,
            title = "Overdue Task",
            deadline = nowMs - 2 * oneDayMs,
            priority = ProjectTaskPriority.LOW
        )

        val rec = ProjectTaskEngine.computeNextAction(listOf(normalTask, overdueTask), nowMs)
        assertEquals("Overdue Task", rec.task?.title)
        assertTrue(rec.reason.contains("Overdue"))
    }

    // 11. High-priority available task beats normal-priority task
    @Test
    fun test11_highPriorityTask_beatsNormalPriorityTask() {
        val normalTask = ProjectTaskDomain(
            id = 1L,
            projectId = 1L,
            title = "Normal Priority",
            priority = ProjectTaskPriority.MEDIUM
        )
        val highTask = ProjectTaskDomain(
            id = 2L,
            projectId = 1L,
            title = "High Priority",
            priority = ProjectTaskPriority.HIGH
        )

        val rec = ProjectTaskEngine.computeNextAction(listOf(normalTask, highTask), nowMs)
        assertEquals("High Priority", rec.task?.title)
        assertTrue(rec.reason.contains("High priority"))
    }

    // 12. Earlier deadline beats later deadline when priority is equal
    @Test
    fun test12_earlierDeadline_beatsLaterDeadlineWhenPriorityIsEqual() {
        val laterTask = ProjectTaskDomain(
            id = 1L,
            projectId = 1L,
            title = "Later Deadline",
            deadline = nowMs + 10 * oneDayMs,
            priority = ProjectTaskPriority.MEDIUM
        )
        val earlierTask = ProjectTaskDomain(
            id = 2L,
            projectId = 1L,
            title = "Earlier Deadline",
            deadline = nowMs + 2 * oneDayMs,
            priority = ProjectTaskPriority.MEDIUM
        )

        val rec = ProjectTaskEngine.computeNextAction(listOf(laterTask, earlierTask), nowMs)
        assertEquals("Earlier Deadline", rec.task?.title)
    }

    // 13. All tasks completed -> Project complete
    @Test
    fun test13_allTasksCompleted_showsProjectComplete() {
        val task1 = ProjectTaskDomain(id = 1L, projectId = 1L, title = "Task 1", completedAt = nowMs)
        val task2 = ProjectTaskDomain(id = 2L, projectId = 1L, title = "Task 2", completedAt = nowMs)

        val rec = ProjectTaskEngine.computeNextAction(listOf(task1, task2), nowMs)
        assertEquals(NextActionStatus.PROJECT_COMPLETE, rec.status)
        assertEquals("Project complete", rec.title)
    }

    // 14. All unfinished tasks blocked -> Waiting on dependencies
    @Test
    fun test14_allUnfinishedBlocked_showsWaitingOnDependencies() {
        // Task 1 was somehow deleted or external, Task 2 depends on 999 (not completed)
        val blockedTask = ProjectTaskDomain(
            id = 2L,
            projectId = 1L,
            title = "Assemble Circuit",
            dependencyTaskId = 999L,
            completedAt = null
        )

        val rec = ProjectTaskEngine.computeNextAction(listOf(blockedTask), nowMs)
        assertEquals(NextActionStatus.WAITING_ON_DEPENDENCIES, rec.status)
        assertEquals("Waiting on dependencies", rec.title)
    }

    // 15. Progress calculation matches progress bar
    @Test
    fun test15_progressCalculation_matchesProgressBar() {
        val project = ProjectDomain(id = 1L, title = "Test", lastActivityAt = 1000L, totalTasks = 4, completedTasks = 1)
        assertEquals(25f, project.progressPercentage, 0.001f)

        val fraction = 1f / 4f
        assertEquals(0.25f, fraction, 0.001f)
    }

    // 16. Existing projects with no dependencies continue working normally
    @Test
    fun test16_existingProjectsWithNoDependencies_workNormally() {
        val legacyTask1 = ProjectTaskDomain(id = 1L, projectId = 1L, title = "Legacy 1", isNextAction = true, sortOrder = 0)
        val legacyTask2 = ProjectTaskDomain(id = 2L, projectId = 1L, title = "Legacy 2", isNextAction = false, sortOrder = 1)

        val rec = ProjectTaskEngine.computeNextAction(listOf(legacyTask1, legacyTask2), nowMs)
        assertEquals(NextActionStatus.AVAILABLE_TASK, rec.status)
        assertEquals("Legacy 1", rec.task?.title)
    }
}
