package com.studentos.core.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class ContestReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsDao: SettingsDao,
    private val notificationDispatcher: NotificationDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val isEnabled = settingsDao.get("notification_contest_reminder_enabled")?.toBooleanStrictOrNull() ?: true
        if (!isEnabled) return Result.success()

        val contestId = inputData.getLong(KEY_CONTEST_ID, -1L)
        val contestName = inputData.getString(KEY_CONTEST_NAME) ?: "Upcoming Contest"
        val platform = inputData.getString(KEY_PLATFORM) ?: "Competitive Programming"
        val contestDate = inputData.getLong(KEY_CONTEST_DATE, 0L)

        val dateText = if (contestDate > 0L) {
            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(contestDate))
        } else {
            "Soon"
        }

        val notificationId = if (contestId > 0L) (300_000 + contestId).toInt() else System.currentTimeMillis().toInt()

        notificationDispatcher.postNotification(
            channelId = NotificationChannelRegistry.CHANNEL_CONTEST_REMINDER,
            notificationId = notificationId,
            title = "Upcoming Contest: $contestName",
            message = "Platform: $platform | Starts at $dateText",
            route = "coding/cp-dashboard"
        )

        return Result.success()
    }

    companion object {
        const val KEY_CONTEST_ID = "contest_id"
        const val KEY_CONTEST_NAME = "contest_name"
        const val KEY_PLATFORM = "platform"
        const val KEY_CONTEST_DATE = "contest_date"
    }
}
