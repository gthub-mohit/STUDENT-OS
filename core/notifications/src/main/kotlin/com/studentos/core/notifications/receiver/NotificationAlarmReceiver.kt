package com.studentos.core.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class NotificationAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationReceiverEntryPoint {
        fun notificationDispatcher(): NotificationDispatcher
    }

    var notificationDispatcher: NotificationDispatcher? = null

    override fun onReceive(context: Context, intent: Intent) {
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: NotificationChannelRegistry.CHANNEL_DAILY_BRIEF
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return
        val route = intent.getStringExtra(EXTRA_ROUTE)

        val dispatcher = notificationDispatcher ?: try {
            val appContext = context.applicationContext ?: context
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                NotificationReceiverEntryPoint::class.java
            )
            entryPoint.notificationDispatcher()
        } catch (_: Exception) {
            null
        }

        dispatcher?.postNotification(
            channelId = channelId,
            notificationId = notificationId,
            title = title,
            message = message,
            route = route
        )
    }

    companion object {
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_ROUTE = "extra_route"
    }
}
