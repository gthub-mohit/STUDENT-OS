package com.studentos.core.notifications.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.DsaTopicEntity
import com.studentos.core.database.entity.ProjectEntity
import com.studentos.core.database.relation.ProjectWithNextAction
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FreeSlotWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val dsaTopicDao: DsaTopicDao = mockk(relaxed = true)
    private val projectDao: ProjectDao = mockk(relaxed = true)
    private val assignmentDao: AssignmentDao = mockk(relaxed = true)
    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val notificationDispatcher: NotificationDispatcher = mockk(relaxed = true)

    private lateinit var worker: FreeSlotWorker

    @Before
    fun setup() {
        worker = FreeSlotWorker(
            context = context,
            params = params,
            dsaTopicDao = dsaTopicDao,
            projectDao = projectDao,
            assignmentDao = assignmentDao,
            settingsDao = settingsDao,
            notificationDispatcher = notificationDispatcher
        )
    }

    @Test
    fun doWork_withDsaTopic_postsDsaRecommendation() = runTest {
        val topic = DsaTopicEntity(
            id = 1L,
            categoryId = 1L,
            name = "Binary Search Trees",
            confidenceLevel = 2,
            revisionStatus = "IN_PROGRESS"
        )
        coEvery { settingsDao.get("notification_free_slot_enabled") } returns "true"
        coEvery { dsaTopicDao.getSuggestedTopic() } returns topic

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            notificationDispatcher.postNotification(
                channelId = NotificationChannelRegistry.CHANNEL_FREE_SLOT_RECOMMENDATION,
                notificationId = FreeSlotWorker.NOTIFICATION_ID,
                title = "Free Slot Available",
                message = match { it.contains("Binary Search Trees") },
                route = "coding/cp-dashboard"
            )
        }
    }

    @Test
    fun doWork_withProjectAction_postsProjectRecommendation() = runTest {
        val projectEntity = ProjectEntity(
            id = 3L,
            title = "Embedded Firmware",
            archivedAt = null,
            inactivityThresholdDays = 7,
            lastActivityAt = System.currentTimeMillis()
        )
        val projectWithAction = ProjectWithNextAction(
            project = projectEntity,
            nextActionId = 12L,
            nextActionTitle = "Write SPI Driver",
            isNextAction = true,
            isParallel = false,
            completedAt = null,
            sortOrder = 1
        )
        coEvery { settingsDao.get("notification_free_slot_enabled") } returns "true"
        coEvery { dsaTopicDao.getSuggestedTopic() } returns null
        coEvery { projectDao.getProjectsWithNextAction() } returns listOf(projectWithAction)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            notificationDispatcher.postNotification(
                channelId = NotificationChannelRegistry.CHANNEL_FREE_SLOT_RECOMMENDATION,
                notificationId = FreeSlotWorker.NOTIFICATION_ID,
                title = "Free Slot Available",
                message = match { it.contains("Write SPI Driver") && it.contains("Embedded Firmware") },
                route = "projects/detail/3"
            )
        }
    }

    @Test
    fun doWork_withNoActionableData_skipsNotificationWithoutFakeData() = runTest {
        coEvery { settingsDao.get("notification_free_slot_enabled") } returns "true"
        coEvery { dsaTopicDao.getSuggestedTopic() } returns null
        coEvery { projectDao.getProjectsWithNextAction() } returns emptyList()
        coEvery { assignmentDao.getUrgentAssignments(any()) } returns emptyList()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) {
            notificationDispatcher.postNotification(any(), any(), any(), any(), any())
        }
    }
}
