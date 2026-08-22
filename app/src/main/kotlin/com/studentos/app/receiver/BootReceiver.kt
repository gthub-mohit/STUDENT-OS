package com.studentos.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.studentos.core.notifications.worker.NotificationRescheduleWorker

/**
 * BootReceiver — receives [Intent.ACTION_BOOT_COMPLETED] after device reboot.
 *
 * Enqueues a one-time [NotificationRescheduleWorker] so all WorkManager jobs
 * for class reminders, assignment reminders, contest reminders, project inactivity,
 * and daily brief generation are reliably restored in the background.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            try {
                val workManager = WorkManager.getInstance(context)
                val request = OneTimeWorkRequestBuilder<NotificationRescheduleWorker>()
                    .addTag(NotificationRescheduleWorker.WORK_NAME)
                    .build()

                workManager.enqueueUniqueWork(
                    NotificationRescheduleWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (_: Exception) {
                // Ignore initialization failures in testing environments
            }
        }
    }
}
