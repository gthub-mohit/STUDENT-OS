package com.studentos.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BootReceiver — receives [Intent.ACTION_BOOT_COMPLETED] after device reboot.
 *
 * Responsibility: trigger [NotificationRescheduler] so all WorkManager jobs
 * for assignment reminders, class reminders, and contest reminders are
 * re-enqueued after the device restarts.
 *
 * This receiver is disabled by default in AndroidManifest.xml
 * (`android:enabled="false"`). It is enabled programmatically in task 7.4
 * once WorkManager and NotificationRescheduler are implemented.
 *
 * Implemented fully in: Task 7.4
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO Task 7.4: Launch NotificationRescheduler on IO dispatcher.
            // goAsync() + coroutineScope pattern or delegate to a one-time Worker.
        }
    }
}
