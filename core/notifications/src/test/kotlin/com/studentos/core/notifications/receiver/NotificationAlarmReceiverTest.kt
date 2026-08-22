package com.studentos.core.notifications.receiver

import android.content.Context
import android.content.Intent
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class NotificationAlarmReceiverTest {

    private val context: Context = mockk(relaxed = true)
    private val notificationDispatcher: NotificationDispatcher = mockk(relaxed = true)
    private lateinit var receiver: NotificationAlarmReceiver

    @Before
    fun setup() {
        receiver = NotificationAlarmReceiver().apply {
            notificationDispatcher = this@NotificationAlarmReceiverTest.notificationDispatcher
        }
    }

    @Test
    fun onReceive_withValidExtras_dispatchesNotification() {
        val intent: Intent = mockk(relaxed = true)
        every { intent.getStringExtra(NotificationAlarmReceiver.EXTRA_CHANNEL_ID) } returns NotificationChannelRegistry.CHANNEL_CLASS_REMINDER
        every { intent.getIntExtra(NotificationAlarmReceiver.EXTRA_NOTIFICATION_ID, 1) } returns 101
        every { intent.getStringExtra(NotificationAlarmReceiver.EXTRA_TITLE) } returns "Upcoming Class"
        every { intent.getStringExtra(NotificationAlarmReceiver.EXTRA_MESSAGE) } returns "Data Structures in 15m"
        every { intent.getStringExtra(NotificationAlarmReceiver.EXTRA_ROUTE) } returns "weekly"

        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            notificationDispatcher.postNotification(
                channelId = NotificationChannelRegistry.CHANNEL_CLASS_REMINDER,
                notificationId = 101,
                title = "Upcoming Class",
                message = "Data Structures in 15m",
                route = "weekly"
            )
        }
    }

    @Test
    fun onReceive_withMissingTitle_doesNotDispatch() {
        val intent: Intent = mockk(relaxed = true)
        every { intent.getStringExtra(NotificationAlarmReceiver.EXTRA_TITLE) } returns null

        receiver.onReceive(context, intent)

        verify(exactly = 0) {
            notificationDispatcher.postNotification(any(), any(), any(), any(), any())
        }
    }
}
