package com.studentos.feature.intelligence.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.notifications.channel.NotificationChannelRegistry
import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import com.studentos.feature.intelligence.orchestrator.IntelligenceOrchestrator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyBriefWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val orchestrator: IntelligenceOrchestrator,
    private val settingsDao: SettingsDao,
    private val notificationDispatcher: NotificationDispatcher,
    private val clock: Clock
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val today = LocalDate.now(clock)

        try {
            orchestrator.generateMorningBrief(today)

            val isEnabled = settingsDao.get("notification_daily_brief_enabled")?.toBooleanStrictOrNull() ?: true
            if (isEnabled) {
                notificationDispatcher.postNotification(
                    channelId = NotificationChannelRegistry.CHANNEL_DAILY_BRIEF,
                    notificationId = 400_001,
                    title = "Your Daily Brief is Ready",
                    message = "Review today's schedule and focus priorities.",
                    route = "intelligence/daily-brief?date=$today"
                )
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "daily_brief_worker"
        private const val MAX_RETRIES = 3

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyBriefWorker>(24, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
