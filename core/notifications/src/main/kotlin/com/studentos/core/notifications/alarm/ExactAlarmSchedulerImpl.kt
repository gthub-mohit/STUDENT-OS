package com.studentos.core.notifications.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.studentos.core.notifications.receiver.NotificationAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExactAlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ExactAlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun scheduleExactAlarm(
        requestCode: Int,
        triggerEpochMs: Long,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        route: String?
    ) {
        val am = alarmManager ?: return

        val now = System.currentTimeMillis()
        if (triggerEpochMs <= now) return

        runCatching {
            val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                putExtra(NotificationAlarmReceiver.EXTRA_CHANNEL_ID, channelId)
                putExtra(NotificationAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(NotificationAlarmReceiver.EXTRA_TITLE, title)
                putExtra(NotificationAlarmReceiver.EXTRA_MESSAGE, message)
                putExtra(NotificationAlarmReceiver.EXTRA_ROUTE, route)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent
                    )
                } else {
                    am.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            } else {
                am.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMs,
                    pendingIntent
                )
            }
        }.onFailure {
            // Fallback for restricted exact alarm permission or test environments
            runCatching {
                val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                    putExtra(NotificationAlarmReceiver.EXTRA_CHANNEL_ID, channelId)
                    putExtra(NotificationAlarmReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                    putExtra(NotificationAlarmReceiver.EXTRA_TITLE, title)
                    putExtra(NotificationAlarmReceiver.EXTRA_MESSAGE, message)
                    putExtra(NotificationAlarmReceiver.EXTRA_ROUTE, route)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent
                    )
                } else {
                    am.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMs,
                        pendingIntent
                    )
                }
            }
        }
    }

    override fun cancelExactAlarm(requestCode: Int) {
        val am = alarmManager ?: return

        runCatching {
            val intent = Intent(context, NotificationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
