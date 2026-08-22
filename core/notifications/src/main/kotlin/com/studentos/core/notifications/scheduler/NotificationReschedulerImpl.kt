package com.studentos.core.notifications.scheduler

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.notifications.alarm.ExactAlarmScheduler
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.worker.ClassReminderWorker
import com.studentos.core.notifications.worker.ProjectInactivityWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationReschedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val classEventDao: ClassEventDao,
    private val subjectDao: SubjectDao,
    private val assignmentDao: AssignmentDao,
    private val cpContestDao: CpContestDao,
    private val settingsDao: SettingsDao,
    private val exactAlarmScheduler: ExactAlarmScheduler
) : NotificationRescheduler {

    override suspend fun rescheduleAll() {
        rescheduleClassReminders()
        rescheduleAssignmentReminders()
        rescheduleContestReminders()
        schedulePeriodicProjectInactivityCheck()
        schedulePeriodicDailyBrief()
    }

    override suspend fun rescheduleClassReminders() {
        val isEnabled = settingsDao.get("notification_class_reminder_enabled")?.toBooleanStrictOrNull() ?: true
        val workManager = getWorkManager()

        if (!isEnabled) {
            return
        }

        val leadMinutes = settingsDao.get("notification_class_reminder_lead_minutes")?.toIntOrNull() ?: 15
        val leadMs = leadMinutes * 60 * 1000L
        val now = System.currentTimeMillis()
        val nextWeekEpoch = now + (7 * 24 * 60 * 60 * 1000L)

        val upcomingEvents = classEventDao.getEventsForWeek(now, nextWeekEpoch).firstOrNull() ?: emptyList()

        for (event in upcomingEvents) {
            val statusUpper = event.status.uppercase().trim()
            if (statusUpper in setOf("CANCELLED", "HOLIDAY", "PRESENT", "ABSENT", "EXTRA_CLASS")) {
                exactAlarmScheduler.cancelExactAlarm((10_000 + event.id).toInt())
                continue
            }

            val triggerEpoch = event.scheduledAt - leadMs
            val delayMs = triggerEpoch - now

            if (delayMs > 0) {
                val subject = subjectDao.getSubjectById(event.subjectId).firstOrNull()
                val subjectName = subject?.name ?: "Class"
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val startTimeStr = timeFormat.format(Date(event.scheduledAt))
                val message = "$subjectName starts at $startTimeStr in $leadMinutes minutes."

                // 1. Schedule exact alarm with AlarmManager for instant background delivery
                exactAlarmScheduler.scheduleExactAlarm(
                    requestCode = (10_000 + event.id).toInt(),
                    triggerEpochMs = triggerEpoch,
                    channelId = NotificationChannelRegistry.CHANNEL_CLASS_REMINDER,
                    notificationId = event.id.toInt(),
                    title = "Upcoming Class",
                    message = message,
                    route = "weekly"
                )

                // 2. Schedule WorkManager request as fallback
                if (workManager != null) {
                    val tag = "class_reminder_${event.id}"
                    val inputData = workDataOf(
                        ClassReminderWorker.KEY_CLASS_EVENT_ID to event.id
                    )
                    val request = OneTimeWorkRequestBuilder<ClassReminderWorker>()
                        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                        .addTag(tag)
                        .setInputData(inputData)
                        .build()

                    workManager.enqueueUniqueWork(
                        tag,
                        ExistingWorkPolicy.REPLACE,
                        request
                    )
                }
            }
        }
    }

    override suspend fun rescheduleAssignmentReminders() {
        val isEnabled = settingsDao.get("notification_assignment_reminder_enabled")?.toBooleanStrictOrNull() ?: true
        val workManager = getWorkManager()

        if (!isEnabled) {
            return
        }

        val defaultLeadMs = settingsDao.get("default_assignment_reminder_lead_ms")?.toLongOrNull() ?: 86_400_000L
        val now = System.currentTimeMillis()
        val allAssignments = assignmentDao.getAllAssignments().firstOrNull() ?: emptyList()

        for (assignment in allAssignments) {
            if (assignment.status == AssignmentEntity.STATUS_SUBMITTED || assignment.status == AssignmentEntity.STATUS_COMPLETED) {
                exactAlarmScheduler.cancelExactAlarm((20_000 + assignment.id).toInt())
                continue
            }

            val leadMs = assignment.reminderLeadMs ?: defaultLeadMs
            val triggerEpoch = assignment.deadline - leadMs
            val delayMs = triggerEpoch - now

            if (delayMs > 0) {
                val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                val deadlineText = dateFormat.format(Date(assignment.deadline))

                // 1. Schedule exact alarm with AlarmManager
                exactAlarmScheduler.scheduleExactAlarm(
                    requestCode = (20_000 + assignment.id).toInt(),
                    triggerEpochMs = triggerEpoch,
                    channelId = NotificationChannelRegistry.CHANNEL_ASSIGNMENT_REMINDER,
                    notificationId = assignment.id.toInt(),
                    title = "Assignment Due Soon: ${assignment.title}",
                    message = "Due at $deadlineText",
                    route = "assignments/list"
                )

                // 2. Schedule WorkManager fallback
                if (workManager != null) {
                    val tag = "assignment_${assignment.id}"
                    val inputData = workDataOf(
                        "assignment_id" to assignment.id,
                        "assignment_title" to assignment.title,
                        "assignment_deadline" to assignment.deadline
                    )

                    val request = try {
                        val workerClass = Class.forName("com.studentos.feature.assignments.worker.AssignmentReminderWorker")
                            .asSubclass(androidx.work.ListenableWorker::class.java)
                        OneTimeWorkRequest.Builder(workerClass)
                            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                            .addTag(tag)
                            .setInputData(inputData)
                            .build()
                    } catch (_: Exception) {
                        null
                    }

                    if (request != null) {
                        workManager.enqueueUniqueWork(
                            tag,
                            ExistingWorkPolicy.REPLACE,
                            request
                        )
                    }
                }
            }
        }
    }

    override suspend fun rescheduleContestReminders() {
        val isEnabled = settingsDao.get("notification_contest_reminder_enabled")?.toBooleanStrictOrNull() ?: true
        val workManager = getWorkManager()

        if (!isEnabled) {
            return
        }

        val lookaheadMs = settingsDao.get("contest_reminder_lookahead_ms")?.toLongOrNull() ?: 86_400_000L
        val now = System.currentTimeMillis()
        val upcomingContests = cpContestDao.getUpcomingContests(now, now + lookaheadMs)

        for (contest in upcomingContests) {
            val delayMs = contest.contestDate - now
            if (delayMs > 0) {
                val dateText = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(contest.contestDate))

                // 1. Schedule exact alarm with AlarmManager
                exactAlarmScheduler.scheduleExactAlarm(
                    requestCode = (30_000 + contest.id).toInt(),
                    triggerEpochMs = contest.contestDate,
                    channelId = NotificationChannelRegistry.CHANNEL_CONTEST_REMINDER,
                    notificationId = (300_000 + contest.id).toInt(),
                    title = "Upcoming Contest: ${contest.contestName}",
                    message = "Platform: CP | Starts at $dateText",
                    route = "coding/cp-dashboard"
                )

                // 2. Schedule WorkManager fallback
                if (workManager != null) {
                    val tag = "contest_${contest.id}"
                    val inputData = workDataOf(
                        "contest_id" to contest.id,
                        "contest_name" to contest.contestName,
                        "platform" to "CP",
                        "contest_date" to contest.contestDate
                    )

                    val request = try {
                        val workerClass = Class.forName("com.studentos.core.sync.worker.ContestReminderWorker")
                            .asSubclass(androidx.work.ListenableWorker::class.java)
                        OneTimeWorkRequest.Builder(workerClass)
                            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                            .addTag(tag)
                            .setInputData(inputData)
                            .build()
                    } catch (_: Exception) {
                        null
                    }

                    if (request != null) {
                        workManager.enqueueUniqueWork(
                            tag,
                            ExistingWorkPolicy.REPLACE,
                            request
                        )
                    }
                }
            }
        }
    }

    override suspend fun schedulePeriodicProjectInactivityCheck() {
        val workManager = getWorkManager() ?: return
        val request = PeriodicWorkRequestBuilder<ProjectInactivityWorker>(24, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            ProjectInactivityWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override suspend fun schedulePeriodicDailyBrief() {
        val workManager = getWorkManager() ?: return
        val request = try {
            val workerClass = Class.forName("com.studentos.feature.intelligence.worker.DailyBriefWorker")
                .asSubclass(androidx.work.ListenableWorker::class.java)
            PeriodicWorkRequest.Builder(workerClass, 24, TimeUnit.HOURS)
                .build()
        } catch (_: Exception) {
            null
        }

        if (request != null) {
            workManager.enqueueUniquePeriodicWork(
                "daily_brief_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    private fun getWorkManager(): WorkManager? {
        return try {
            WorkManager.getInstance(context)
        } catch (_: Exception) {
            null
        }
    }
}
