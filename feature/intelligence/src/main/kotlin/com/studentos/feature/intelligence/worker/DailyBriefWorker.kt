package com.studentos.feature.intelligence.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import com.studentos.feature.intelligence.domain.usecase.GenerateDailyBriefUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyBriefWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val generateDailyBriefUseCase: GenerateDailyBriefUseCase,
    private val repository: DailyBriefRepository,
    private val clock: Clock
) : CoroutineWorker(context, params) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val today = LocalDate.now(clock).format(dateFormatter)

        try {
            val existing = repository.getBriefForDate(today).firstOrNull()
            if (existing != null) {
                return@withContext Result.success()
            }

            generateDailyBriefUseCase(today)
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
