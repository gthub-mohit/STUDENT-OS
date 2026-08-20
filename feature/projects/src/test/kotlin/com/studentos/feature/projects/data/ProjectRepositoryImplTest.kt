package com.studentos.feature.projects.data

import app.cash.turbine.test
import com.studentos.core.database.dao.BugDao
import com.studentos.core.database.dao.MilestoneDao
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.ProjectResourceDao
import com.studentos.core.database.dao.ProjectTaskDao
import com.studentos.core.database.entity.BugEntity
import com.studentos.core.database.entity.MilestoneEntity
import com.studentos.core.database.entity.ProjectEntity
import com.studentos.core.database.entity.ProjectResourceEntity
import com.studentos.core.database.entity.ProjectTaskEntity
import com.studentos.core.events.AppEventBus
import com.studentos.feature.projects.data.repository.ProjectRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectRepositoryImplTest {

    private val projectDao: ProjectDao = mockk(relaxed = true)
    private val projectTaskDao: ProjectTaskDao = mockk(relaxed = true)
    private val milestoneDao: MilestoneDao = mockk(relaxed = true)
    private val bugDao: BugDao = mockk(relaxed = true)
    private val projectResourceDao: ProjectResourceDao = mockk(relaxed = true)
    private val appEventBus: AppEventBus = mockk(relaxed = true)
    private val fixedInstant = Instant.parse("2026-08-01T10:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

    private lateinit var repository: ProjectRepositoryImpl

    @Before
    fun setUp() {
        repository = ProjectRepositoryImpl(
            projectDao,
            projectTaskDao,
            milestoneDao,
            bugDao,
            projectResourceDao,
            appEventBus,
            clock
        )
    }

    @Test
    fun getActiveProjects_mapsEntitiesAndTasksToDomain() = runTest {
        val projectEntity = ProjectEntity(
            id = 1L,
            title = "Compiler Project",
            lastActivityAt = 1000L,
            inactivityThresholdDays = 7
        )
        val task1 = ProjectTaskEntity(id = 10L, projectId = 1L, title = "Lexer Parser", isNextAction = true, completedAt = 1000L)
        val task2 = ProjectTaskEntity(id = 11L, projectId = 1L, title = "Codegen", isNextAction = false, completedAt = null)

        every { projectDao.getActiveProjects() } returns flowOf(listOf(projectEntity))
        every { projectTaskDao.getTasksForProject(1L) } returns flowOf(listOf(task1, task2))
        every { projectTaskDao.getNextAction(1L) } returns flowOf(task1)

        repository.getActiveProjects().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            val domain = list[0]
            assertEquals("Compiler Project", domain.title)
            assertEquals(2, domain.totalTasks)
            assertEquals(1, domain.completedTasks)
            assertEquals(50f, domain.progressPercentage, 0.01f)
            assertEquals("Lexer Parser", domain.nextActionTitle)
            awaitComplete()
        }
    }

    @Test
    fun createProject_insertsProjectEntityWithClockTimestamp() = runTest {
        coEvery { projectDao.insert(any()) } returns 101L

        val id = repository.createProject("New Website", 14)

        assertEquals(101L, id)
        coVerify {
            projectDao.insert(
                match {
                    it.title == "New Website" &&
                            it.inactivityThresholdDays == 14 &&
                            it.lastActivityAt == fixedInstant.toEpochMilli()
                }
            )
        }
    }

    @Test
    fun createTask_setsNextActionIfNoExistingNextAction() = runTest {
        coEvery { projectTaskDao.getMaxSortOrder(1L) } returns 0
        every { projectTaskDao.getNextAction(1L) } returns flowOf(null)
        coEvery { projectTaskDao.insert(any()) } returns 201L

        val taskId = repository.createTask(1L, "Design Architecture", false)

        assertEquals(201L, taskId)
        coVerify {
            projectTaskDao.insert(
                match {
                    it.projectId == 1L &&
                            it.title == "Design Architecture" &&
                            it.isNextAction &&
                            it.sortOrder == 1
                }
            )
        }
    }

    @Test
    fun createTask_withDependencyAndPriority_insertsCorrectEntity() = runTest {
        coEvery { projectTaskDao.getMaxSortOrder(1L) } returns 1
        every { projectTaskDao.getNextAction(1L) } returns flowOf(mockk())
        coEvery { projectTaskDao.insert(any()) } returns 202L

        val taskId = repository.createTask(
            projectId = 1L,
            title = "Assemble Circuit",
            isParallel = false,
            dependencyTaskId = 10L,
            priority = com.studentos.feature.projects.domain.model.ProjectTaskPriority.HIGH,
            deadline = 9999L
        )

        assertEquals(202L, taskId)
        coVerify {
            projectTaskDao.insert(
                match {
                    it.projectId == 1L &&
                            it.title == "Assemble Circuit" &&
                            it.dependencyTaskId == 10L &&
                            it.priority == "HIGH" &&
                            it.deadline == 9999L &&
                            it.sortOrder == 2
                }
            )
        }
    }

    @Test
    fun deleteTask_clearsDependencyReferencesAndDeletes() = runTest {
        val existingTask = ProjectTaskEntity(id = 10L, projectId = 1L, title = "Task to delete")
        coEvery { projectTaskDao.getTaskById(10L) } returns existingTask

        repository.deleteTask(10L)

        coVerify {
            projectTaskDao.clearDependencyReferences(10L)
            projectTaskDao.deleteById(10L)
        }
    }

    @Test
    fun setNextAction_clearsExistingNextActionFirstToPreserveInvariant() = runTest {
        repository.setNextAction(1L, 12L)

        coVerify {
            projectTaskDao.clearNextActionForProject(1L)
            projectTaskDao.updateNextAction(12L, true)
        }
    }

    @Test
    fun completeTask_promotesNextUnfinishedTask() = runTest {
        val activeTask = ProjectTaskEntity(id = 10L, projectId = 1L, title = "Task 1", isNextAction = true)
        val nextTask = ProjectTaskEntity(id = 11L, projectId = 1L, title = "Task 2", isNextAction = false)

        coEvery { projectTaskDao.getTaskById(10L) } returns activeTask
        coEvery { projectTaskDao.getFirstUnfinishedTask(1L) } returns nextTask

        repository.completeTask(10L)

        coVerify {
            projectTaskDao.updateCompletedAt(10L, fixedInstant.toEpochMilli())
            projectTaskDao.updateNextAction(11L, true)
        }
    }

    @Test
    fun createMilestone_insertsMilestoneEntityAndUpdatesProjectActivity() = runTest {
        coEvery { milestoneDao.insert(any()) } returns 301L

        val milestoneId = repository.createMilestone(1L, "Release v1.0", "MVP launch", 5000L)

        assertEquals(301L, milestoneId)
        coVerify {
            milestoneDao.insert(
                match {
                    it.projectId == 1L &&
                            it.title == "Release v1.0" &&
                            it.description == "MVP launch" &&
                            it.targetDate == 5000L &&
                            it.status == MilestoneEntity.STATUS_PENDING
                }
            )
            projectDao.updateLastActivityAt(1L, fixedInstant.toEpochMilli())
        }
    }

    @Test
    fun completeMilestone_updatesStatusToDone() = runTest {
        val milestone = MilestoneEntity(id = 301L, projectId = 1L, title = "Release v1.0")
        coEvery { milestoneDao.getMilestoneById(301L) } returns milestone

        repository.completeMilestone(301L)

        coVerify {
            milestoneDao.updateStatus(301L, MilestoneEntity.STATUS_DONE)
            projectDao.updateLastActivityAt(1L, fixedInstant.toEpochMilli())
        }
    }

    @Test
    fun createBug_insertsBugEntityWithOpenStatus() = runTest {
        coEvery { bugDao.insert(any()) } returns 401L

        val bugId = repository.createBug(1L, "Crash on back button", "HIGH")

        assertEquals(401L, bugId)
        coVerify {
            bugDao.insert(
                match {
                    it.projectId == 1L &&
                            it.description == "Crash on back button" &&
                            it.severity == "HIGH" &&
                            it.status == BugEntity.STATUS_OPEN
                }
            )
            projectDao.updateLastActivityAt(1L, fixedInstant.toEpochMilli())
        }
    }

    @Test
    fun resolveBug_updatesStatusToResolved() = runTest {
        val bug = BugEntity(id = 401L, projectId = 1L, description = "Crash on back button")
        coEvery { bugDao.getBugById(401L) } returns bug

        repository.resolveBug(401L)

        coVerify {
            bugDao.updateStatus(401L, BugEntity.STATUS_RESOLVED)
            projectDao.updateLastActivityAt(1L, fixedInstant.toEpochMilli())
        }
    }

    @Test
    fun createResource_insertsProjectResourceEntityAndEmitsProjectUpdated() = runTest {
        coEvery { projectResourceDao.insert(any()) } returns 501L

        val resourceId = repository.createResource(1L, "https://github.com", "GitHub Repo", "LINK")

        assertEquals(501L, resourceId)
        coVerify {
            projectResourceDao.insert(
                match {
                    it.projectId == 1L &&
                            it.url == "https://github.com" &&
                            it.label == "GitHub Repo"
                }
            )
            projectDao.updateLastActivityAt(1L, fixedInstant.toEpochMilli())
        }
    }

    @Test
    fun deleteResource_deletesResourceAndUpdatesProjectActivity() = runTest {
        val resource = ProjectResourceEntity(id = 501L, projectId = 1L, url = "https://github.com")
        coEvery { projectResourceDao.getResourceById(501L) } returns resource

        repository.deleteResource(501L)

        coVerify {
            projectResourceDao.deleteById(501L)
            projectDao.updateLastActivityAt(1L, fixedInstant.toEpochMilli())
        }
    }
}
