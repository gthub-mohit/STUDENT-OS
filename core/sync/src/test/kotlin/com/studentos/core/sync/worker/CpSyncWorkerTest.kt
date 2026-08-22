package com.studentos.core.sync.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.CpContestEntity
import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.database.entity.SettingEntity
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.sync.api.CodeChefApiService
import com.studentos.core.sync.api.CodeforcesApiService
import com.studentos.core.sync.api.dto.CodeChefContestDto
import com.studentos.core.sync.api.dto.CodeChefProfileResponseDto
import com.studentos.core.sync.api.dto.CodeforcesContestDto
import com.studentos.core.sync.api.dto.CodeforcesProfileDto
import com.studentos.core.sync.api.dto.CodeforcesRatingResponseDto
import com.studentos.core.sync.api.dto.CodeforcesUserResponseDto
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class CpSyncWorkerTest {

    private class FakeAppEventBus : AppEventBus {
        val emittedEvents = mutableListOf<AppEvent>()
        private val _events = MutableSharedFlow<AppEvent>(replay = 10)
        override val events: SharedFlow<AppEvent> = _events.asSharedFlow()

        override suspend fun emit(event: AppEvent) {
            emittedEvents.add(event)
            _events.emit(event)
        }
    }

    private class FakeCpProfileDao(
        val profiles: MutableList<CpProfileEntity> = mutableListOf()
    ) : CpProfileDao {
        var lastUpserted: CpProfileEntity? = null

        override suspend fun upsert(profile: CpProfileEntity): Long {
            lastUpserted = profile
            val index = profiles.indexOfFirst { it.id == profile.id || it.platform == profile.platform }
            if (index >= 0) {
                profiles[index] = profile
            } else {
                profiles.add(profile)
            }
            return profile.id
        }

        override suspend fun updateRatingAndSyncTime(id: Long, rating: Int?, syncTime: Long) {}
        override fun getProfileByPlatform(platform: String): Flow<CpProfileEntity?> = flowOf(profiles.firstOrNull { it.platform == platform })
        override suspend fun getProfileByPlatformOnce(platform: String): CpProfileEntity? = profiles.firstOrNull { it.platform == platform }
        override suspend fun deleteByPlatform(platform: String) { profiles.removeAll { it.platform == platform } }
        override fun getAllProfiles(): Flow<List<CpProfileEntity>> = flowOf(profiles)
        override suspend fun getProfilesForSnapshot(): List<CpProfileEntity> = profiles.toList()
    }

    private class FakeCpContestDao(
        val contests: MutableList<CpContestEntity> = mutableListOf()
    ) : CpContestDao {
        var lastUpsertedContests: List<CpContestEntity>? = null

        override suspend fun upsertContests(contests: List<CpContestEntity>): List<Long> {
            lastUpsertedContests = contests
            this.contests.addAll(contests)
            return contests.map { it.id }
        }

        override fun getContestsByProfile(profileId: Long): Flow<List<CpContestEntity>> = flowOf(contests.filter { it.profileId == profileId })
        override fun getRecentContests(profileId: Long, limit: Int): Flow<List<CpContestEntity>> = flowOf(contests.filter { it.profileId == profileId })
        override fun getAllContests(): Flow<List<CpContestEntity>> = flowOf(contests)

        override suspend fun getUpcomingContests(nowEpoch: Long, lookaheadEpoch: Long): List<CpContestEntity> {
            return contests.filter { it.contestDate in nowEpoch..lookaheadEpoch }
        }
    }

    private class FakeSettingsDao(
        private val settingsMap: MutableMap<String, String> = mutableMapOf()
    ) : SettingsDao {
        override suspend fun get(key: String): String? = settingsMap[key]
        override suspend fun getAll(): List<SettingEntity> = settingsMap.map { SettingEntity(it.key, it.value) }
        override fun observeAll(): kotlinx.coroutines.flow.Flow<List<SettingEntity>> =
            kotlinx.coroutines.flow.flowOf(settingsMap.map { SettingEntity(it.key, it.value) })
        override suspend fun set(setting: SettingEntity) {
            settingsMap[setting.key] = setting.value
        }
    }

    private class FakeCodeChefApiService : CodeChefApiService {
        var shouldFail = false
        var isInvalidHandle = false

        override suspend fun getUserProfile(handle: String): CodeChefProfileResponseDto {
            if (shouldFail) throw IOException("Network error")
            if (isInvalidHandle) throw IOException("HTTP 404 User not found")

            return CodeChefProfileResponseDto(
                status = "success",
                handle = handle,
                currentRating = 1800,
                ratingData = listOf(
                    CodeChefContestDto(
                        code = "START100",
                        name = "Starters 100",
                        rank = 10,
                        ratingChange = 50,
                        getRatedDate = System.currentTimeMillis() - 86400000L
                    )
                )
            )
        }
    }

    private class FakeCodeforcesApiService : CodeforcesApiService {
        var shouldFail = false
        var isInvalidHandle = false

        override suspend fun getUserInfo(handle: String): CodeforcesUserResponseDto {
            if (shouldFail) throw IOException("Network error")
            if (isInvalidHandle) {
                return CodeforcesUserResponseDto(status = "FAILED", comment = "User not found")
            }
            return CodeforcesUserResponseDto(
                status = "OK",
                result = listOf(
                    CodeforcesProfileDto(handle = handle, rating = 1600, maxRating = 1650, rank = "specialist")
                )
            )
        }

        override suspend fun getUserRating(handle: String): CodeforcesRatingResponseDto {
            if (shouldFail) throw IOException("Network error")
            if (isInvalidHandle) {
                return CodeforcesRatingResponseDto(status = "FAILED", comment = "User not found")
            }
            return CodeforcesRatingResponseDto(
                status = "OK",
                result = listOf(
                    CodeforcesContestDto(
                        contestId = 500L,
                        contestName = "Codeforces Round 800",
                        rank = 20,
                        ratingUpdateTimeSeconds = (System.currentTimeMillis() - 86400000L) / 1000L,
                        oldRating = 1550,
                        newRating = 1600
                    )
                )
            )
        }
    }

    private class MinimalTestContext : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    private lateinit var profileDao: FakeCpProfileDao
    private lateinit var contestDao: FakeCpContestDao
    private lateinit var settingsDao: FakeSettingsDao
    private lateinit var codeChefApi: FakeCodeChefApiService
    private lateinit var codeforcesApi: FakeCodeforcesApiService
    private lateinit var eventBus: FakeAppEventBus

    private var scheduledTag: String? = null
    private var scheduledDelay: Long? = null

    @Before
    fun setUp() {
        profileDao = FakeCpProfileDao()
        contestDao = FakeCpContestDao()
        settingsDao = FakeSettingsDao()
        codeChefApi = FakeCodeChefApiService()
        codeforcesApi = FakeCodeforcesApiService()
        eventBus = FakeAppEventBus()

        scheduledTag = null
        scheduledDelay = null
    }

    private fun createWorker(): CpSyncWorker {
        val params = mockk<WorkerParameters>(relaxed = true)
        return CpSyncWorker(
            MinimalTestContext(),
            params,
            profileDao,
            contestDao,
            settingsDao,
            codeChefApi,
            codeforcesApi,
            eventBus
        ).apply {
            scheduleContestReminderDelegate = { tag, delayMs, _, _ ->
                scheduledTag = tag
                scheduledDelay = delayMs
            }
        }
    }

    @Test
    fun doWork_noConfiguredProfiles_exitsSuccessfullyWithoutEvents() = runBlocking {
        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        assertTrue(eventBus.emittedEvents.isEmpty())
    }

    @Test
    fun doWork_successfulSync_upsertsDataUpdatesSyncTimeAndEmitsEvent() = runBlocking {
        val profile = CpProfileEntity(id = 1L, platform = CpProfileEntity.PLATFORM_CODECHEF, handle = "chef123")
        profileDao.profiles.add(profile)

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        assertNotNull(profileDao.lastUpserted)
        assertEquals(1800, profileDao.lastUpserted?.currentRating)
        assertNotNull(profileDao.lastUpserted?.lastSyncedAt)
        assertTrue((profileDao.lastUpserted?.lastSyncedAt ?: 0L) > 0L)

        assertEquals(1, eventBus.emittedEvents.size)
        assertTrue(eventBus.emittedEvents[0] is AppEvent.CpSyncCompleted)
    }

    @Test
    fun doWork_apiFailure_preservesLocalRoomData() = runBlocking {
        val initialProfile = CpProfileEntity(
            id = 1L,
            platform = CpProfileEntity.PLATFORM_CODECHEF,
            handle = "chef123",
            currentRating = 1750,
            lastSyncedAt = 1000L
        )
        profileDao.profiles.add(initialProfile)
        codeChefApi.shouldFail = true

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        // Profile in DB must remain unmodified
        assertEquals(1750, profileDao.profiles[0].currentRating)
        assertEquals(1000L, profileDao.profiles[0].lastSyncedAt)
        // Event bus should not emit on total failure
        assertTrue(eventBus.emittedEvents.isEmpty())
    }

    @Test
    fun doWork_partialPlatformFailure_syncsWorkingPlatformAndPreservesFailedOne() = runBlocking {
        profileDao.profiles.add(CpProfileEntity(id = 1L, platform = CpProfileEntity.PLATFORM_CODECHEF, handle = "chef123", currentRating = 1700))
        profileDao.profiles.add(CpProfileEntity(id = 2L, platform = CpProfileEntity.PLATFORM_CODEFORCES, handle = "cf123", currentRating = 1500))

        codeChefApi.shouldFail = true // CodeChef fails

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)

        // CodeChef profile unchanged
        assertEquals(1700, profileDao.profiles.first { it.platform == CpProfileEntity.PLATFORM_CODECHEF }.currentRating)

        // Codeforces profile updated
        assertEquals(1600, profileDao.profiles.first { it.platform == CpProfileEntity.PLATFORM_CODEFORCES }.currentRating)

        // Sync completed event emitted because at least one platform succeeded
        assertEquals(1, eventBus.emittedEvents.size)
        assertTrue(eventBus.emittedEvents[0] is AppEvent.CpSyncCompleted)
    }

    @Test
    fun doWork_upcomingContestInLookahead_schedulesContestReminder() = runBlocking {
        profileDao.profiles.add(CpProfileEntity(id = 1L, platform = CpProfileEntity.PLATFORM_CODECHEF, handle = "chef123"))

        val now = System.currentTimeMillis()
        val futureContest = CpContestEntity(
            id = 99L,
            profileId = 1L,
            contestName = "Starters 200",
            contestDate = now + 3600000L // 1 hour in future
        )
        contestDao.contests.add(futureContest)
        settingsDao.set(SettingEntity("contest_reminder_lookahead_ms", "86400000")) // 24h lookahead

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        assertEquals("contest_99", scheduledTag)
        assertNotNull(scheduledDelay)
        assertTrue((scheduledDelay ?: 0L) in 3500000L..3700000L)
    }

    @Test
    fun doWork_noUpcomingContests_skipsContestReminderScheduling() = runBlocking {
        profileDao.profiles.add(CpProfileEntity(id = 1L, platform = CpProfileEntity.PLATFORM_CODECHEF, handle = "chef123"))

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        assertFalse(scheduledTag != null)
    }

    @Test
    fun doWork_pastContest_skipsScheduling() = runBlocking {
        profileDao.profiles.add(CpProfileEntity(id = 1L, platform = CpProfileEntity.PLATFORM_CODECHEF, handle = "chef123"))

        val now = System.currentTimeMillis()
        val pastContest = CpContestEntity(
            id = 88L,
            profileId = 1L,
            contestName = "Old Contest",
            contestDate = now - 10000L // In the past
        )
        contestDao.contests.add(pastContest)

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
        assertFalse(scheduledTag != null)
    }
}
