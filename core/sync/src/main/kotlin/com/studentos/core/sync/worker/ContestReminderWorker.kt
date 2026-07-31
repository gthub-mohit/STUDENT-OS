package com.studentos.core.sync.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class ContestReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val contestId = inputData.getLong(KEY_CONTEST_ID, -1L)
        val contestName = inputData.getString(KEY_CONTEST_NAME) ?: "Upcoming Contest"
        val platform = inputData.getString(KEY_PLATFORM) ?: "Competitive Programming"
        val contestDate = inputData.getLong(KEY_CONTEST_DATE, 0L)

        postNotification(contestId, contestName, platform, contestDate)
        return Result.success()
    }

    private fun postNotification(contestId: Long, contestName: String, platform: String, contestDate: Long) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Contest Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val dateText = if (contestDate > 0L) {
            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(contestDate))
        } else {
            "Soon"
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Upcoming Contest: $contestName")
            .setContentText("Platform: $platform | Starts at $dateText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationId = if (contestId > 0L) contestId.toInt() else System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "CONTEST_REMINDER"
        const val KEY_CONTEST_ID = "contest_id"
        const val KEY_CONTEST_NAME = "contest_name"
        const val KEY_PLATFORM = "platform"
        const val KEY_CONTEST_DATE = "contest_date"
    }
}
