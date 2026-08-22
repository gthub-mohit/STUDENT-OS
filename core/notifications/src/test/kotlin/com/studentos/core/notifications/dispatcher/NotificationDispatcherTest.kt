package com.studentos.core.notifications.dispatcher

import android.app.NotificationManager
import android.content.Context
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class NotificationDispatcherTest {

    private val context: Context = mockk(relaxed = true)
    private val notificationManager: NotificationManager = mockk(relaxed = true)
    private lateinit var dispatcher: NotificationDispatcherImpl

    @Before
    fun setup() {
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { context.packageName } returns "com.studentos"
        dispatcher = NotificationDispatcherImpl(context)
    }

    @Test
    fun postNotification_withoutCrashing_handlesSafeExecution() {
        val posted = dispatcher.postNotification(
            channelId = NotificationChannelRegistry.CHANNEL_CLASS_REMINDER,
            notificationId = 101,
            title = "Class Reminder",
            message = "Operating Systems starts in 15 minutes",
            route = "weekly"
        )
        // Verifies non-crashing execution
        assertNotNull(posted)
    }

    @Test
    fun cancelNotification_callsManagerCancel() {
        dispatcher.cancelNotification(101)
        verify { notificationManager.cancel(101) }
    }
}
