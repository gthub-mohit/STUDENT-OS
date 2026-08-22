package com.studentos.core.sync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.CpContestEntity
import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.sync.api.CodeChefApiService
import com.studentos.core.sync.api.CodeforcesApiService
import com.studentos.core.sync.mapper.CodeChefMapper
import com.studentos.core.sync.mapper.CodeforcesMapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * CpSyncWorker — WorkManager periodic worker for competitive programming profile & contest synchronization.
 *
 * NOTE: Contest reminders are best-effort. WorkManager with NETWORK_REQUIRED constraint is used;
 * Doze-mode delays of up to 15 minutes are accepted as documented in design.md.
 */
@HiltWorker
class CpSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cpProfileDao: CpProfileDao,
    private val cpContestDao: CpContestDao,
    private val settingsDao: SettingsDao,
    private val codeChefApiService: CodeChefApiService,
    private val codeforcesApiService: CodeforcesApiService,
    private val eventBus: AppEventBus
) : CoroutineWorker(context, params) {

    // Test delegate to intercept WorkManager scheduling in JVM unit tests
    var scheduleContestReminderDelegate: ((tag: String, delayMs: Long, contestId: Long, contestName: String) -> Unit)? = null

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val profiles = cpProfileDao.getProfilesForSnapshot()
        if (profiles.isEmpty()) {
            return@withContext Result.success()
        }

        val now = System.currentTimeMillis()
        var atLeastOneSuccess = false

        for (profile in profiles) {
            when (profile.platform.uppercase()) {
                CpProfileEntity.PLATFORM_CODECHEF -> {
                    try {
                        val response = codeChefApiService.getUserProfile(profile.handle)
                        val mappedProfile = CodeChefMapper.mapProfile(
                            dto = response,
                            fallbackHandle = profile.handle,
                            existingId = profile.id,
                            syncedAtMs = now
                        )
                        if (mappedProfile != null) {
                            cpProfileDao.upsert(mappedProfile)
                        }

                        val mappedContests = CodeChefMapper.mapContests(
                            dtos = response.ratingData,
                            profileId = profile.id
                        )
                        if (mappedContests.isNotEmpty()) {
                            cpContestDao.upsertContests(mappedContests)
                        }
                        atLeastOneSuccess = true
                    } catch (_: Exception) {
                        // Preserve existing Room records on platform API error
                    }
                }

                CpProfileEntity.PLATFORM_CODEFORCES -> {
                    try {
                        val userInfo = codeforcesApiService.getUserInfo(profile.handle)
                        val userRating = codeforcesApiService.getUserRating(profile.handle)

                        val userDto = userInfo.result?.firstOrNull()
                        val mappedProfile = CodeforcesMapper.mapProfile(
                            dto = userDto,
                            fallbackHandle = profile.handle,
                            existingId = profile.id,
                            syncedAtMs = now
                        )
                        if (mappedProfile != null) {
                            cpProfileDao.upsert(mappedProfile)
                        }

                        val mappedContests = CodeforcesMapper.mapContests(
                            dtos = userRating.result,
                            profileId = profile.id
                        )
                        if (mappedContests.isNotEmpty()) {
                            cpContestDao.upsertContests(mappedContests)
                        }
                        atLeastOneSuccess = true
                    } catch (_: Exception) {
                        // Preserve existing Room records on platform API error
                    }
                }
            }
        }

        // Schedule contest reminders for upcoming contests in lookahead window
        scheduleUpcomingContestReminders(now, profiles)

        // Emit AppEvent.CpSyncCompleted on completion
        if (atLeastOneSuccess) {
            eventBus.emit(AppEvent.CpSyncCompleted)
        }

        Result.success()
    }

    private suspend fun scheduleUpcomingContestReminders(now: Long, profiles: List<CpProfileEntity>) {
        val lookaheadMs = settingsDao.get("contest_reminder_lookahead_ms")?.toLongOrNull()
            ?: DEFAULT_LOOKAHEAD_MS
        val lookaheadEpoch = now + lookaheadMs

        val upcomingContests = cpContestDao.getUpcomingContests(now, lookaheadEpoch)
        if (upcomingContests.isEmpty()) return

        val profileMap = profiles.associateBy { it.id }

        for (contest in upcomingContests) {
            val delayMs = contest.contestDate - now
            if (delayMs <= 0) continue

            val platform = profileMap[contest.profileId]?.platform ?: "CP"
            val tag = "contest_${contest.id}"

            if (scheduleContestReminderDelegate != null) {
                scheduleContestReminderDelegate?.invoke(tag, delayMs, contest.id, contest.contestName)
            } else {
                try {
                    val workManager = WorkManager.getInstance(applicationContext)
                    val inputData = workDataOf(
                        ContestReminderWorker.KEY_CONTEST_ID to contest.id,
                        ContestReminderWorker.KEY_CONTEST_NAME to contest.contestName,
                        ContestReminderWorker.KEY_PLATFORM to platform,
                        ContestReminderWorker.KEY_CONTEST_DATE to contest.contestDate
                    )
                    val workRequest = OneTimeWorkRequestBuilder<ContestReminderWorker>()
                        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                        .addTag(tag)
                        .setInputData(inputData)
                        .build()

                    workManager.enqueueUniqueWork(
                        tag,
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                } catch (_: Exception) {
                    // Ignore WorkManager initialization errors during non-Android execution
                }
            }
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "cp_sync_periodic"
        const val WORK_NAME_IMMEDIATE = "cp_sync_immediate"
        const val DEFAULT_LOOKAHEAD_MS = 86400000L // 24 hours

        fun enqueueImmediate(context: Context) {
            try {
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                val request = OneTimeWorkRequestBuilder<CpSyncWorker>()
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME_IMMEDIATE,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (_: Exception) {
                // Ignore during test execution
            }
        }

        fun enqueuePeriodic(context: Context, intervalMinutes: Long = 360L) {
            try {
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                val request = androidx.work.PeriodicWorkRequestBuilder<CpSyncWorker>(
                    intervalMinutes.coerceAtLeast(15L),
                    TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } catch (_: Exception) {
                // Ignore during test execution
            }
        }
    }
}
