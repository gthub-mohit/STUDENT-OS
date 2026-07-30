package com.studentos.feature.assignments.data.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.feature.assignments.domain.scheduler.AssignmentReminderScheduler
import com.studentos.feature.assignments.worker.AssignmentReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AssignmentReminderSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDao: SettingsDao
) : AssignmentReminderScheduler {

    var enqueueWorkDelegate: ((tag: String, delayMs: Long, assignmentId: Long) -> Unit)? = null
    var cancelWorkDelegate: ((tag: String) -> Unit)? = null

    override suspend fun scheduleReminder(assignment: AssignmentEntity) {
        val tag = "assignment_${assignment.id}"

        if (assignment.status == AssignmentEntity.STATUS_SUBMITTED || assignment.status == AssignmentEntity.STATUS_COMPLETED) {
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
            cancelWork(tag)
            return
        }

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
