package com.studentos.core.notifications.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.ProjectDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.SettingEntity
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ProjectInactivityWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val projectDao: ProjectDao,
    private val settingsDao: SettingsDao,
    private val notificationDispatcher: NotificationDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val isEnabled = settingsDao.get("notification_inactive_project_enabled")?.toBooleanStrictOrNull() ?: true
        if (!isEnabled) return Result.success()

        val activeProjects = projectDao.getActiveProjectsForInactivityCheck()
        if (activeProjects.isEmpty()) return Result.success()

        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        for (project in activeProjects) {
            val thresholdDays = project.inactivityThresholdDays.coerceAtLeast(1)
            val thresholdMs = thresholdDays * oneDayMs
            val elapsedMs = now - project.lastActivityAt

            if (elapsedMs >= thresholdMs) {
                // Check 24h cooldown to prevent repeated daily spam for the same project
                val cooldownKey = "last_inactivity_notified_${project.id}"
                val lastNotified = settingsDao.get(cooldownKey)?.toLongOrNull() ?: 0L
                if (now - lastNotified < oneDayMs) {
                    continue
                }

                val inactiveDays = (elapsedMs / oneDayMs).toInt()
                val message = "You haven't worked on ${project.title} for $inactiveDays days."

                notificationDispatcher.postNotification(
                    channelId = NotificationChannelRegistry.CHANNEL_INACTIVE_PROJECT_REMINDER,
                    notificationId = 100_000 + project.id.toInt(),
                    title = "Project Needs Attention",
                    message = message,
                    route = "projects/detail/${project.id}"
                )

                settingsDao.set(SettingEntity(key = cooldownKey, value = now.toString()))
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "periodic_project_inactivity"
    }
}
