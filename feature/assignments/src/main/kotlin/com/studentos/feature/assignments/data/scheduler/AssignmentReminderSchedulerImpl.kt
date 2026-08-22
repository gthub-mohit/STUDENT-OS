package com.studentos.feature.assignments.data.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.notifications.alarm.ExactAlarmScheduler
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.feature.assignments.domain.scheduler.AssignmentReminderScheduler
import com.studentos.feature.assignments.worker.AssignmentReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AssignmentReminderSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao,
    private val exactAlarmScheduler: ExactAlarmScheduler
) : AssignmentReminderScheduler {

    var enqueueWorkDelegate: ((tag: String, delayMs: Long, assignmentId: Long) -> Unit)? = null
    var cancelWorkDelegate: ((tag: String) -> Unit)? = null

    override suspend fun scheduleReminder(assignment: AssignmentEntity) {
        val tag = "assignment_${assignment.id}"
        val alarmRequestCode = (20_000 + assignment.id).toInt()

        if (assignment.status == AssignmentEntity.STATUS_SUBMITTED || assignment.status == AssignmentEntity.STATUS_COMPLETED) {
            exactAlarmScheduler.cancelExactAlarm(alarmRequestCode)
            cancelWork(tag)
            return
        }

        val isEnabled = settingsDao.get("notification_assignment_reminder_enabled")?.toBooleanStrictOrNull() ?: true
        if (!isEnabled) {
            exactAlarmScheduler.cancelExactAlarm(alarmRequestCode)
            cancelWork(tag)
            return
        }

        val leadMs = assignment.reminderLeadMs
            ?: settingsDao.get("default_assignment_reminder_lead_ms")?.toLongOrNull()
            ?: DEFAULT_REMINDER_LEAD_MS

        val triggerEpoch = assignment.deadline - leadMs
        val nowEpoch = System.currentTimeMillis()
        val delayMs = triggerEpoch - nowEpoch

        if (delayMs <= 0) {
            exactAlarmScheduler.cancelExactAlarm(alarmRequestCode)
            cancelWork(tag)
            return
        }

        val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        val deadlineText = dateFormat.format(Date(assignment.deadline))

        // 1. Schedule exact alarm with AlarmManager for immediate background delivery
        exactAlarmScheduler.scheduleExactAlarm(
            requestCode = alarmRequestCode,
            triggerEpochMs = triggerEpoch,
            channelId = NotificationChannelRegistry.CHANNEL_ASSIGNMENT_REMINDER,
            notificationId = assignment.id.toInt(),
            title = "Assignment Due Soon: ${assignment.title}",
            message = "Due at $deadlineText",
            route = "assignments/list"
        )

        // 2. Schedule WorkManager fallback
        if (enqueueWorkDelegate != null) {
            enqueueWorkDelegate?.invoke(tag, delayMs, assignment.id)
            return
        }

        val workManager = try {
            WorkManager.getInstance(context)
        } catch (_: Exception) {
            return
        }

        val inputData = workDataOf(
            AssignmentReminderWorker.KEY_ASSIGNMENT_ID to assignment.id,
            AssignmentReminderWorker.KEY_ASSIGNMENT_TITLE to assignment.title,
            AssignmentReminderWorker.KEY_ASSIGNMENT_DEADLINE to assignment.deadline
        )

        val workRequest = OneTimeWorkRequestBuilder<AssignmentReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(tag)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            tag,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override suspend fun cancelReminder(assignmentId: Long) {
        val tag = "assignment_$assignmentId"
        val alarmRequestCode = (20_000 + assignmentId).toInt()
        exactAlarmScheduler.cancelExactAlarm(alarmRequestCode)
        cancelWork(tag)
    }

    private fun cancelWork(tag: String) {
        if (cancelWorkDelegate != null) {
            cancelWorkDelegate?.invoke(tag)
            return
        }
        try {
            WorkManager.getInstance(context).cancelAllWorkByTag(tag)
        } catch (_: Exception) {
            // Ignore
        }
    }

    companion object {
        const val DEFAULT_REMINDER_LEAD_MS = 3600000L // 1 hour
    }
}
