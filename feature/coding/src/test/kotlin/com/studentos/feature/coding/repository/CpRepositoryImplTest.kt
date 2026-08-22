package com.studentos.feature.coding.repository

import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.CpReflectionDao
import com.studentos.core.database.entity.CpContestEntity
import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.database.entity.CpReflectionEntity
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppResult
import com.studentos.feature.coding.data.repository.CpRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CpRepositoryImplTest {

    private class FakeAppEventBus : AppEventBus {
        private val _events = MutableSharedFlow<AppEvent>()
        override val events: SharedFlow<AppEvent> = _events.asSharedFlow()

        override suspend fun emit(event: AppEvent) {
            _events.emit(event)
        }
    }

    private class FakeCpProfileDao : CpProfileDao {
        val profiles = mutableListOf<CpProfileEntity>()

        override suspend fun upsert(profile: CpProfileEntity): Long {
            profiles.removeAll { it.platform == profile.platform }
            profiles.add(profile)
            return 1L
        }

        override suspend fun updateRatingAndSyncTime(id: Long, rating: Int?, syncTime: Long) {
            val existing = profiles.find { it.id == id }
            if (existing != null) {
                profiles.remove(existing)
                profiles.add(existing.copy(currentRating = rating, lastSyncedAt = syncTime))
            }
        }

        override fun getProfileByPlatform(platform: String): Flow<CpProfileEntity?> {
            return flowOf(profiles.find { it.platform == platform })
        }

        override suspend fun getProfileByPlatformOnce(platform: String): CpProfileEntity? {
            return profiles.find { it.platform == platform }
        }

        override suspend fun deleteByPlatform(platform: String) {
            profiles.removeAll { it.platform == platform }
        }

        override fun getAllProfiles(): Flow<List<CpProfileEntity>> = flowOf(profiles)
        override suspend fun getProfilesForSnapshot(): List<CpProfileEntity> = profiles
    }

    private class FakeCpContestDao : CpContestDao {
        val contests = mutableListOf<CpContestEntity>()

        override suspend fun upsertContests(contests: List<CpContestEntity>): List<Long> {
            this.contests.addAll(contests)
            return contests.map { it.id }
        }

        override fun getContestsByProfile(profileId: Long): Flow<List<CpContestEntity>> =
            flowOf(contests.filter { it.profileId == profileId })

        override fun getRecentContests(profileId: Long, limit: Int): Flow<List<CpContestEntity>> =
            flowOf(contests.filter { it.profileId == profileId }.take(limit))

        override fun getAllContests(): Flow<List<CpContestEntity>> = flowOf(contests)

        override suspend fun getUpcomingContests(fromEpochMs: Long, toEpochMs: Long): List<CpContestEntity> = emptyList()
    }

    private class FakeCpReflectionDao : CpReflectionDao {
        val reflections = mutableListOf<CpReflectionEntity>()

        override suspend fun insert(reflection: CpReflectionEntity): Long {
            reflections.add(reflection)
            return reflection.id
        }

        override suspend fun update(reflection: CpReflectionEntity) {
            reflections.removeAll { it.contestId == reflection.contestId }
            reflections.add(reflection)
        }

        override fun getReflectionForContest(contestId: Long): Flow<CpReflectionEntity?> =
            flowOf(reflections.find { it.contestId == contestId })
    }

    @Test
    fun addOrUpdateProfile_persistsProfileToDao() = runBlocking {
        val profileDao = FakeCpProfileDao()
        val contestDao = FakeCpContestDao()
        val reflectionDao = FakeCpReflectionDao()
        val eventBus = FakeAppEventBus()

        val repo = CpRepositoryImpl(profileDao, contestDao, reflectionDao, eventBus)

        val result = repo.addOrUpdateProfile("CODEFORCES", "tourist")
        assertTrue(result is AppResult.Success)

        val savedProfiles = repo.getProfiles().first()
        assertEquals(1, savedProfiles.size)
        assertEquals("CODEFORCES", savedProfiles[0].platform)
        assertEquals("tourist", savedProfiles[0].handle)
    }

    @Test
    fun getAllContests_returnsContestsFromDao() = runBlocking {
        val profileDao = FakeCpProfileDao()
        val contestDao = FakeCpContestDao()
        val reflectionDao = FakeCpReflectionDao()
        val eventBus = FakeAppEventBus()

        contestDao.upsertContests(
            listOf(
                CpContestEntity(id = 1L, profileId = 10L, contestName = "Starters 100", contestDate = 1000L),
                CpContestEntity(id = 2L, profileId = 20L, contestName = "Round 900", contestDate = 2000L)
            )
        )

        val repo = CpRepositoryImpl(profileDao, contestDao, reflectionDao, eventBus)
        val allContests = repo.getAllContests().first()
        assertEquals(2, allContests.size)
    }
}
