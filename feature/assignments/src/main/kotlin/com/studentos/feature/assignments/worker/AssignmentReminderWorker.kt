package com.studentos.feature.assignments.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.entity.AssignmentEntity
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
    private val assignmentDao: AssignmentDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val assignmentId = inputData.getLong(KEY_ASSIGNMENT_ID, -1L)
        if (assignmentId == -1L) return Result.failure()

        val assignment = assignmentDao.getAssignmentById(assignmentId).firstOrNull()
            ?: return Result.success()

        // Only fire notification if assignment is still active
        if (assignment.status == AssignmentEntity.STATUS_PENDING || assignment.status == AssignmentEntity.STATUS_IN_PROGRESS) {
            postNotification(assignment)
        }

        return Result.success()
    }

    private fun postNotification(assignment: AssignmentEntity) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Assignment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        val deadlineText = dateFormat.format(Date(assignment.deadline))

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Assignment Due Soon: ${assignment.title}")
            .setContentText("Due at $deadlineText")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(assignment.id.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "ASSIGNMENT_REMINDER"
        const val KEY_ASSIGNMENT_ID = "assignment_id"
        const val KEY_ASSIGNMENT_TITLE = "assignment_title"
        const val KEY_ASSIGNMENT_DEADLINE = "assignment_deadline"
    }
}
