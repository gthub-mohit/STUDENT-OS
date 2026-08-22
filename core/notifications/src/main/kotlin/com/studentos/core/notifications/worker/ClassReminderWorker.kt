package com.studentos.core.notifications.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class ClassReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val classEventDao: ClassEventDao,
    private val subjectDao: SubjectDao,
    private val settingsDao: SettingsDao,
    private val notificationDispatcher: NotificationDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val eventId = inputData.getLong(KEY_CLASS_EVENT_ID, -1L)
        if (eventId == -1L) return Result.failure()

        // 1. Check if class reminders are enabled
        val isEnabled = settingsDao.get("notification_class_reminder_enabled")?.toBooleanStrictOrNull() ?: true
        if (!isEnabled) return Result.success()

        // 2. Fetch event and validate status
        val event = classEventDao.getEventByIdOnce(eventId) ?: return Result.success()

        // Skip if already marked, cancelled, or holiday
        val statusUpper = event.status.uppercase().trim()
        if (statusUpper in setOf("CANCELLED", "HOLIDAY", "PRESENT", "ABSENT", "EXTRA_CLASS")) {
            return Result.success()
        }

        // Skip if class is already past
        val now = System.currentTimeMillis()
        if (event.scheduledAt < now - 60_000L) {
            return Result.success()
        }

        // 3. Resolve Subject and Location
        val subject = subjectDao.getSubjectById(event.subjectId).firstOrNull()
        val subjectName = subject?.name ?: "Class"
        val location = inputData.getString(KEY_LOCATION)?.takeIf { it.isNotBlank() }

        val leadMinutes = settingsDao.get("notification_class_reminder_lead_minutes")?.toIntOrNull() ?: 15
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val startTimeStr = timeFormat.format(Date(event.scheduledAt))

        val message = buildString {
            append("$subjectName starts at $startTimeStr")
            if (location != null) {
                append(" in $location")
            }
            append(" in $leadMinutes minutes.")
        }

        // 4. Dispatch notification
        notificationDispatcher.postNotification(
            channelId = NotificationChannelRegistry.CHANNEL_CLASS_REMINDER,
            notificationId = event.id.toInt(),
            title = "Upcoming Class",
            message = message,
            route = "weekly"
        )

        return Result.success()
    }

    companion object {
        const val KEY_CLASS_EVENT_ID = "class_event_id"
        const val KEY_LOCATION = "location"
    }
}
