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

        override fun getAllProfiles(): Flow<List<CpProfileEntity>> = flowOf(profiles)
        override suspend fun getProfilesForSnapshot(): List<CpProfileEntity> = profiles
    }

    private class FakeCpContestDao : CpContestDao {
        override suspend fun upsertContests(contests: List<CpContestEntity>): List<Long> = emptyList()
        override fun getContestsByProfile(profileId: Long): Flow<List<CpContestEntity>> = flowOf(emptyList())
        override fun getRecentContests(profileId: Long, limit: Int): Flow<List<CpContestEntity>> = flowOf(emptyList())
        override suspend fun getUpcomingContests(fromEpochMs: Long, toEpochMs: Long): List<CpContestEntity> = emptyList()
    }

    private class FakeCpReflectionDao : CpReflectionDao {
        override suspend fun insert(reflection: CpReflectionEntity): Long = 1L
        override suspend fun update(reflection: CpReflectionEntity) {}
        override fun getReflectionForContest(contestId: Long): Flow<CpReflectionEntity?> = flowOf(null)
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
}
