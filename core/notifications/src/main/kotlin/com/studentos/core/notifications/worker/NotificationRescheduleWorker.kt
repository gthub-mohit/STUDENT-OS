package com.studentos.core.notifications.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studentos.core.notifications.scheduler.NotificationRescheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NotificationRescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationRescheduler: NotificationRescheduler
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            notificationRescheduler.rescheduleAll()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "notification_reschedule_worker"
    }
}
