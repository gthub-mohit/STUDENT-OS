package com.studentos.core.notifications.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class FreeSlotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dsaTopicDao: DsaTopicDao,
    private val projectDao: ProjectDao,
    private val assignmentDao: AssignmentDao,
    private val settingsDao: SettingsDao,
    private val notificationDispatcher: NotificationDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val isEnabled = settingsDao.get("notification_free_slot_enabled")?.toBooleanStrictOrNull() ?: true
        if (!isEnabled) return Result.success()

        val now = System.currentTimeMillis()

        // 1. Check lowest confidence DSA topic
        val suggestedDsaTopic = dsaTopicDao.getSuggestedTopic()
        if (suggestedDsaTopic != null) {
            notificationDispatcher.postNotification(
                channelId = NotificationChannelRegistry.CHANNEL_FREE_SLOT_RECOMMENDATION,
                notificationId = NOTIFICATION_ID,
                title = "Free Slot Available",
                message = "You have time for practice. Continue ${suggestedDsaTopic.name} revision.",
                route = "coding/cp-dashboard"
            )
            return Result.success()
        }

        // 2. Check active project next action
        val activeProjectsWithAction = projectDao.getProjectsWithNextAction()
            .filter { it.nextActionTitle != null }
        val projectWithAction = activeProjectsWithAction.firstOrNull()
        if (projectWithAction != null && projectWithAction.nextActionTitle != null) {
            notificationDispatcher.postNotification(
                channelId = NotificationChannelRegistry.CHANNEL_FREE_SLOT_RECOMMENDATION,
                notificationId = NOTIFICATION_ID,
                title = "Free Slot Available",
                message = "Your next project action is '${projectWithAction.nextActionTitle}' on ${projectWithAction.project.title}.",
                route = "projects/detail/${projectWithAction.project.id}"
            )
            return Result.success()
        }

        // 3. Check urgent upcoming assignment
        val urgentAssignments = assignmentDao.getUrgentAssignments(now + 24 * 3600 * 1000L)
        val urgentAssignment = urgentAssignments.firstOrNull()
        if (urgentAssignment != null) {
            notificationDispatcher.postNotification(
                channelId = NotificationChannelRegistry.CHANNEL_FREE_SLOT_RECOMMENDATION,
                notificationId = NOTIFICATION_ID,
                title = "Free Slot Available",
                message = "Use your free time to work on assignment: ${urgentAssignment.title}.",
                route = "assignments/list"
            )
            return Result.success()
        }

        return Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 200_001
        const val WORK_NAME = "free_slot_recommendation_worker"
    }
}
