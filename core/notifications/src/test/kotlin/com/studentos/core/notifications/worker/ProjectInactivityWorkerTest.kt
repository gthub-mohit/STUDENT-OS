package com.studentos.core.notifications.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.ProjectEntity
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProjectInactivityWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val projectDao: ProjectDao = mockk(relaxed = true)
    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val notificationDispatcher: NotificationDispatcher = mockk(relaxed = true)

    private lateinit var worker: ProjectInactivityWorker

    @Before
    fun setup() {
        worker = ProjectInactivityWorker(
            context = context,
            params = params,
            projectDao = projectDao,
            settingsDao = settingsDao,
            notificationDispatcher = notificationDispatcher
        )
    }

    @Test
    fun doWork_withInactiveProject_postsNotification() = runTest {
        val now = System.currentTimeMillis()
        val eightDaysAgo = now - (8 * 24 * 60 * 60 * 1000L)
        val inactiveProject = ProjectEntity(
            id = 5L,
            title = "Compiler Project",
            inactivityThresholdDays = 7,
            lastActivityAt = eightDaysAgo
        )

        coEvery { settingsDao.get("notification_inactive_project_enabled") } returns "true"
        coEvery { settingsDao.get("last_inactivity_notified_5") } returns null
        coEvery { projectDao.getActiveProjectsForInactivityCheck() } returns listOf(inactiveProject)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            notificationDispatcher.postNotification(
                channelId = NotificationChannelRegistry.CHANNEL_INACTIVE_PROJECT_REMINDER,
                notificationId = 100_005,
                title = "Project Needs Attention",
                message = match { it.contains("Compiler Project") && it.contains("8 days") },
                route = "projects/detail/5"
            )
        }
    }

    @Test
    fun doWork_withRecentlyActiveProject_skipsNotification() = runTest {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L)
        val activeProject = ProjectEntity(
            id = 6L,
            title = "Active App",
            inactivityThresholdDays = 7,
            lastActivityAt = twoDaysAgo
        )

        coEvery { settingsDao.get("notification_inactive_project_enabled") } returns "true"
        coEvery { projectDao.getActiveProjectsForInactivityCheck() } returns listOf(activeProject)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) {
            notificationDispatcher.postNotification(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun doWork_withinCooldownPeriod_skipsNotification() = runTest {
        val now = System.currentTimeMillis()
        val eightDaysAgo = now - (8 * 24 * 60 * 60 * 1000L)
        val threeHoursAgo = now - (3 * 60 * 60 * 1000L)
        val inactiveProject = ProjectEntity(
            id = 7L,
            title = "Robotics OS",
            inactivityThresholdDays = 7,
            lastActivityAt = eightDaysAgo
        )

        coEvery { settingsDao.get("notification_inactive_project_enabled") } returns "true"
        coEvery { settingsDao.get("last_inactivity_notified_7") } returns threeHoursAgo.toString()
        coEvery { projectDao.getActiveProjectsForInactivityCheck() } returns listOf(inactiveProject)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) {
            notificationDispatcher.postNotification(any(), any(), any(), any(), any())
        }
    }
}
