package com.studentos.core.notifications.dispatcher

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationDispatcherImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationDispatcher {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    override fun postNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        route: String?
    ): Boolean {
        if (notificationManager == null) return false

        return runCatching {
            // 1. Android 13+ (API 33+) Runtime Permission Check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }

            // 2. Ensure channels are initialized
            NotificationChannelRegistry.createAll(context)

            // 3. Build Deep-Link PendingIntent
            val pendingIntent = if (route != null) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("studentos://$route")).apply {
                    `package` = context.packageName
                    putExtra("route", route)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                null
            }

            // 4. Determine Priority from Channel Config
            val channelConfig = NotificationChannelRegistry.ALL_CHANNELS.find { it.id == channelId }
            val priority = when (channelConfig?.importance) {
                NotificationManager.IMPORTANCE_HIGH -> NotificationCompat.PRIORITY_HIGH
                NotificationManager.IMPORTANCE_LOW -> NotificationCompat.PRIORITY_LOW
                NotificationManager.IMPORTANCE_MIN -> NotificationCompat.PRIORITY_MIN
                else -> NotificationCompat.PRIORITY_DEFAULT
            }

            // 5. Build and Dispatch Notification
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(priority)
                .setAutoCancel(true)
                .apply {
                    if (pendingIntent != null) {
                        setContentIntent(pendingIntent)
                    }
                }
                .build()

            notificationManager?.notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    override fun cancelNotification(notificationId: Int) {
        runCatching {
            notificationManager?.cancel(notificationId)
        }
    }
}
