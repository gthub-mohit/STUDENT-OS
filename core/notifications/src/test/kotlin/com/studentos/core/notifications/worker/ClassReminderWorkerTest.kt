package com.studentos.core.notifications.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ClassReminderWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val params: WorkerParameters = mockk(relaxed = true)
    private val classEventDao: ClassEventDao = mockk(relaxed = true)
    private val subjectDao: SubjectDao = mockk(relaxed = true)
    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val notificationDispatcher: NotificationDispatcher = mockk(relaxed = true)

    private lateinit var worker: ClassReminderWorker

    @Before
    fun setup() {
        worker = ClassReminderWorker(
            context = context,
            params = params,
            classEventDao = classEventDao,
            subjectDao = subjectDao,
            settingsDao = settingsDao,
            notificationDispatcher = notificationDispatcher
        )
    }

    @Test
    fun doWork_withValidUpcomingClass_postsNotification() = runTest {
        val futureTime = System.currentTimeMillis() + 900_000L // 15 min in future
        val event = ClassEventEntity(
            id = 10L,
            subjectId = 1L,
            scheduledAt = futureTime,
            endAt = futureTime + 3600_000L,
            status = "UNMARKED",
            updatedAt = System.currentTimeMillis()
        )
        val subject = SubjectEntity(id = 1L, name = "Data Structures")

        coEvery { params.inputData } returns workDataOf(
            ClassReminderWorker.KEY_CLASS_EVENT_ID to 10L,
            ClassReminderWorker.KEY_LOCATION to "Room 301"
        )
        coEvery { settingsDao.get("notification_class_reminder_enabled") } returns "true"
        coEvery { settingsDao.get("notification_class_reminder_lead_minutes") } returns "15"
        coEvery { classEventDao.getEventByIdOnce(10L) } returns event
        coEvery { subjectDao.getSubjectById(1L) } returns flowOf(subject)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            notificationDispatcher.postNotification(
                channelId = NotificationChannelRegistry.CHANNEL_CLASS_REMINDER,
                notificationId = 10,
                title = "Upcoming Class",
                message = match { it.contains("Data Structures") && it.contains("Room 301") },
                route = "weekly"
            )
        }
    }

    @Test
    fun doWork_withCancelledClass_skipsNotification() = runTest {
        val futureTime = System.currentTimeMillis() + 900_000L
        val event = ClassEventEntity(
            id = 11L,
            subjectId = 1L,
            scheduledAt = futureTime,
            endAt = futureTime + 3600_000L,
            status = "CANCELLED",
            updatedAt = System.currentTimeMillis()
        )

        coEvery { params.inputData } returns workDataOf(ClassReminderWorker.KEY_CLASS_EVENT_ID to 11L)
        coEvery { settingsDao.get("notification_class_reminder_enabled") } returns "true"
        coEvery { classEventDao.getEventByIdOnce(11L) } returns event

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) {
            notificationDispatcher.postNotification(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun doWork_withPastClass_skipsNotification() = runTest {
        val pastTime = System.currentTimeMillis() - 3600_000L // 1 hour ago
        val event = ClassEventEntity(
            id = 12L,
            subjectId = 1L,
            scheduledAt = pastTime,
            endAt = pastTime + 3600_000L,
            status = "UNMARKED",
            updatedAt = System.currentTimeMillis()
        )

        coEvery { params.inputData } returns workDataOf(ClassReminderWorker.KEY_CLASS_EVENT_ID to 12L)
        coEvery { settingsDao.get("notification_class_reminder_enabled") } returns "true"
        coEvery { classEventDao.getEventByIdOnce(12L) } returns event

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) {
            notificationDispatcher.postNotification(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun doWork_whenNotificationsDisabled_skipsNotification() = runTest {
        coEvery { params.inputData } returns workDataOf(ClassReminderWorker.KEY_CLASS_EVENT_ID to 10L)
        coEvery { settingsDao.get("notification_class_reminder_enabled") } returns "false"

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) {
            notificationDispatcher.postNotification(any(), any(), any(), any(), any())
        }
    }
}
