package com.studentos.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.scheduler.NotificationRescheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * StudentOsApp — Application class for Student OS.
 *
 * Responsibilities:
 *  - Initialises Hilt dependency injection.
 *  - Configures WorkManager with HiltWorkerFactory.
 *  - Registers all 6 Android Notification Channels via [NotificationChannelRegistry].
 *  - Reschedules missing notification jobs via [NotificationRescheduler].
 */
@HiltAndroidApp
class StudentOsApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationRescheduler: NotificationRescheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() {
            val builder = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
            return if (::workerFactory.isInitialized) {
                builder.setWorkerFactory(workerFactory).build()
            } else {
                builder.build()
            }
        }

    override fun onCreate() {
        super.onCreate()

        // 1. Register all Android Notification Channels
        NotificationChannelRegistry.createAll(this)

        // 2. Reschedule notification workers on IO dispatcher
        applicationScope.launch {
            try {
                notificationRescheduler.rescheduleAll()
            } catch (_: Exception) {
                // Ignore initialization failures in testing environments
            }
        }
    }
}
