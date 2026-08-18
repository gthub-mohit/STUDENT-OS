package com.studentos.app

import android.app.Application
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp

import androidx.hilt.work.HiltWorkerFactory
import javax.inject.Inject

/**
 * StudentOsApp — Application class for Student OS.
 *
 * Responsibilities (in the order they are fulfilled across tasks):
 *  - Task 0.1: Annotated with @HiltAndroidApp to initialise the Hilt
 *              dependency injection component graph. Implements
 *              [Configuration.Provider] to support custom WorkManager init.
 *  - Task 0.3: [workerFactory] is @Inject-ed; [workManagerConfiguration]
 *              is updated to use .setWorkerFactory(workerFactory).
 *  - Task 7.1: [NotificationChannelRegistry.createAll] called in onCreate().
 *  - Task 7.4: [NotificationRescheduler] launched on IO dispatcher in onCreate().
 *
 * No business logic lives here. This class only bootstraps infrastructure.
 *
 * WorkManager is initialised manually (the default startup initializer is
 * removed in AndroidManifest.xml) so Hilt can supply the WorkerFactory in
 * task 0.3 without a circular initialization dependency.
 */
@HiltAndroidApp
class StudentOsApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Custom WorkManager configuration.
     *
     * Returns a [Configuration] with minimal logging and [HiltWorkerFactory],
     * enabling Hilt injection in all [androidx.work.ListenableWorker] subclasses.
     */
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
        // Task 7.1: NotificationChannelRegistry.createAll(this)
        // Task 7.4: launch NotificationRescheduler on Dispatchers.IO
    }
}
