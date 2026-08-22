package com.studentos.core.notifications.channel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannelRegistry {

    const val CHANNEL_DAILY_BRIEF = "DAILY_BRIEF"
    const val CHANNEL_ASSIGNMENT_REMINDER = "ASSIGNMENT_REMINDER"
    const val CHANNEL_CLASS_REMINDER = "CLASS_REMINDER"
    const val CHANNEL_CONTEST_REMINDER = "CONTEST_REMINDER"
    const val CHANNEL_FREE_SLOT_RECOMMENDATION = "FREE_SLOT_RECOMMENDATION"
    const val CHANNEL_INACTIVE_PROJECT_REMINDER = "INACTIVE_PROJECT_REMINDER"

    data class ChannelConfig(
        val id: String,
        val name: String,
        val description: String,
        val importance: Int
    )

    val ALL_CHANNELS = listOf(
        ChannelConfig(
            id = CHANNEL_DAILY_BRIEF,
            name = "Daily Brief",
            description = "Morning briefing and day summary",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        ),
        ChannelConfig(
            id = CHANNEL_ASSIGNMENT_REMINDER,
            name = "Assignment Reminders",
            description = "Upcoming task and assignment deadlines",
            importance = NotificationManager.IMPORTANCE_HIGH
        ),
        ChannelConfig(
            id = CHANNEL_CLASS_REMINDER,
            name = "Class Reminders",
            description = "Upcoming class schedule reminders",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        ),
        ChannelConfig(
            id = CHANNEL_CONTEST_REMINDER,
            name = "Contest Reminders",
            description = "Competitive programming contest alerts",
            importance = NotificationManager.IMPORTANCE_HIGH
        ),
        ChannelConfig(
            id = CHANNEL_FREE_SLOT_RECOMMENDATION,
            name = "Free Slot Recommendations",
            description = "Contextual suggestions during free periods",
            importance = NotificationManager.IMPORTANCE_LOW
        ),
        ChannelConfig(
            id = CHANNEL_INACTIVE_PROJECT_REMINDER,
            name = "Inactive Project Reminders",
            description = "Alerts for projects needing attention",
            importance = NotificationManager.IMPORTANCE_DEFAULT
        )
    )

    /**
     * Registers all notification channels with the system. Idempotent and safe to call
     * on every application startup.
     */
    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channels = ALL_CHANNELS.map { config ->
            NotificationChannel(config.id, config.name, config.importance).apply {
                description = config.description
                setShowBadge(true)
            }
        }

        notificationManager.createNotificationChannels(channels)
    }
}
