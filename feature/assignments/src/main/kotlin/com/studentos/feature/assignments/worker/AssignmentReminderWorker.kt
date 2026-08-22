package com.studentos.feature.assignments.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class AssignmentReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val assignmentDao: AssignmentDao,
    private val settingsDao: SettingsDao,
    private val notificationDispatcher: NotificationDispatcher
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val assignmentId = inputData.getLong(KEY_ASSIGNMENT_ID, -1L)
        if (assignmentId == -1L) return Result.failure()

        // 1. Check if assignment notifications are enabled
        val isEnabled = settingsDao.get("notification_assignment_reminder_enabled")?.toBooleanStrictOrNull() ?: true
        if (!isEnabled) return Result.success()

        // 2. Fetch assignment and verify status
        val assignment = assignmentDao.getAssignmentById(assignmentId).firstOrNull()
            ?: return Result.success()

        // Only fire notification if assignment is still active
        if (assignment.status == AssignmentEntity.STATUS_PENDING || assignment.status == AssignmentEntity.STATUS_IN_PROGRESS) {
            val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            val deadlineText = dateFormat.format(Date(assignment.deadline))

            notificationDispatcher.postNotification(
                channelId = NotificationChannelRegistry.CHANNEL_ASSIGNMENT_REMINDER,
                notificationId = assignment.id.toInt(),
                title = "Assignment Due Soon: ${assignment.title}",
                message = "Due at $deadlineText",
                route = "assignments/list"
            )
        }

        return Result.success()
    }

    companion object {
        const val KEY_ASSIGNMENT_ID = "assignment_id"
        const val KEY_ASSIGNMENT_TITLE = "assignment_title"
        const val KEY_ASSIGNMENT_DEADLINE = "assignment_deadline"
    }
}
