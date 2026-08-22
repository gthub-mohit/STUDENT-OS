package com.studentos.core.notifications.channel

import android.app.NotificationManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NotificationChannelRegistryTest {

    @Test
    fun allChannels_containsExactlySixRequiredChannels() {
        assertEquals(6, NotificationChannelRegistry.ALL_CHANNELS.size)

        val channelIds = NotificationChannelRegistry.ALL_CHANNELS.map { it.id }.toSet()
        val expectedIds = setOf(
            NotificationChannelRegistry.CHANNEL_DAILY_BRIEF,
            NotificationChannelRegistry.CHANNEL_ASSIGNMENT_REMINDER,
            NotificationChannelRegistry.CHANNEL_CLASS_REMINDER,
            NotificationChannelRegistry.CHANNEL_CONTEST_REMINDER,
            NotificationChannelRegistry.CHANNEL_FREE_SLOT_RECOMMENDATION,
            NotificationChannelRegistry.CHANNEL_INACTIVE_PROJECT_REMINDER
        )

        assertEquals(expectedIds, channelIds)
    }

    @Test
    fun channelImportances_matchSpecifications() {
        val dailyBrief = NotificationChannelRegistry.ALL_CHANNELS.find { it.id == NotificationChannelRegistry.CHANNEL_DAILY_BRIEF }
        assertNotNull(dailyBrief)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, dailyBrief?.importance)

        val assignment = NotificationChannelRegistry.ALL_CHANNELS.find { it.id == NotificationChannelRegistry.CHANNEL_ASSIGNMENT_REMINDER }
        assertNotNull(assignment)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, assignment?.importance)

        val classReminder = NotificationChannelRegistry.ALL_CHANNELS.find { it.id == NotificationChannelRegistry.CHANNEL_CLASS_REMINDER }
        assertNotNull(classReminder)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, classReminder?.importance)

        val contest = NotificationChannelRegistry.ALL_CHANNELS.find { it.id == NotificationChannelRegistry.CHANNEL_CONTEST_REMINDER }
        assertNotNull(contest)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, contest?.importance)

        val freeSlot = NotificationChannelRegistry.ALL_CHANNELS.find { it.id == NotificationChannelRegistry.CHANNEL_FREE_SLOT_RECOMMENDATION }
        assertNotNull(freeSlot)
        assertEquals(NotificationManager.IMPORTANCE_LOW, freeSlot?.importance)

        val projectInactivity = NotificationChannelRegistry.ALL_CHANNELS.find { it.id == NotificationChannelRegistry.CHANNEL_INACTIVE_PROJECT_REMINDER }
        assertNotNull(projectInactivity)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, projectInactivity?.importance)
    }

    @Test
    fun createAll_isIdempotentAndSafe() {
        val mockContext: Context = mockk(relaxed = true)
        val mockNotificationManager: NotificationManager = mockk(relaxed = true)
        every { mockContext.getSystemService(Context.NOTIFICATION_SERVICE) } returns mockNotificationManager

        // Call twice to verify repeated calls don't crash or fail
        NotificationChannelRegistry.createAll(mockContext)
        NotificationChannelRegistry.createAll(mockContext)

        // Verifies safe execution
    }
}
