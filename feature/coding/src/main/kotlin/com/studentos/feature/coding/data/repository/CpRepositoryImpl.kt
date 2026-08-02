package com.studentos.feature.coding.data.repository

import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.CpReflectionDao
import com.studentos.core.database.entity.CpContestEntity
import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.database.entity.CpReflectionEntity
import com.studentos.core.events.AppError
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppResult
import com.studentos.core.sync.api.CodeChefApiService
import com.studentos.core.sync.api.CodeforcesApiService
import com.studentos.core.sync.mapper.CodeChefMapper
import com.studentos.core.sync.mapper.CodeforcesMapper
import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.CpProfile
import com.studentos.feature.coding.domain.model.CpReflection
import com.studentos.feature.coding.domain.repository.CpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class CpRepositoryImpl @Inject constructor(
    private val cpProfileDao: CpProfileDao,
    private val cpContestDao: CpContestDao,
    private val cpReflectionDao: CpReflectionDao,
    private val eventBus: AppEventBus,
    private val codeChefApiService: CodeChefApiService? = null,
    private val codeforcesApiService: CodeforcesApiService? = null
) : CpRepository {

    override fun getProfiles(): Flow<List<CpProfile>> {
        return cpProfileDao.getAllProfiles().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getContests(profileId: Long): Flow<List<CpContest>> {
        return cpContestDao.getContestsByProfile(profileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllContests(): Flow<List<CpContest>> {
        return cpProfileDao.getAllProfiles().flatMapLatest { profiles ->
            if (profiles.isEmpty()) {
                flowOf(emptyList())
            } else {
                val profileId = profiles.first().id
                cpContestDao.getContestsByProfile(profileId).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }
    }

    override fun getReflection(contestId: Long): Flow<CpReflection?> {
        return cpReflectionDao.getReflectionForContest(contestId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun saveReflection(reflection: CpReflection) {
        val entity = reflection.toEntity()
        if (reflection.id > 0) {
            cpReflectionDao.update(entity)
        } else {
            try {
                cpReflectionDao.insert(entity)
            } catch (_: Exception) {
                cpReflectionDao.update(entity)
            }
        }
    }

    override suspend fun syncProfiles(): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val profiles = cpProfileDao.getProfilesForSnapshot()
            if (profiles.isEmpty()) return@withContext AppResult.Success(Unit)

            val now = System.currentTimeMillis()
            var syncedCount = 0

            for (profile in profiles) {
                val result = fetchAndPersistProfile(profile.platform, profile.handle, profile.id, now)
                if (result is AppResult.Success) {
                    syncedCount++
                }
            }

            if (syncedCount > 0) {
                eventBus.emit(AppEvent.CpSyncCompleted)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.NetworkError(e.message ?: "Failed to sync profiles"))
        }
    }

    override suspend fun syncProfile(platform: String, handle: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingList = cpProfileDao.getProfilesForSnapshot()
            val existing = existingList.find { it.platform.equals(platform, ignoreCase = true) }
            val existingId = existing?.id ?: 0L

            val now = System.currentTimeMillis()
            val result = fetchAndPersistProfile(platform, handle, existingId, now)

            if (result is AppResult.Success) {
                eventBus.emit(AppEvent.CpSyncCompleted)
            }
            result
        } catch (e: Exception) {
            AppResult.Failure(AppError.NetworkError(e.message ?: "Failed to sync profile for $platform"))
        }
    }

    override suspend fun addOrUpdateProfile(platform: String, handle: String): AppResult<Long> = withContext(Dispatchers.IO) {
        try {
            val existingList = cpProfileDao.getProfilesForSnapshot()
            val existing = existingList.find { it.platform.equals(platform, ignoreCase = true) }

            val entity = CpProfileEntity(
                id = existing?.id ?: 0L,
                platform = platform.uppercase(),
                handle = handle,
                currentRating = existing?.currentRating,
                highestRating = existing?.highestRating,
                rank = existing?.rank,
                problemsSolved = existing?.problemsSolved,
                contestCount = existing?.contestCount,
                lastSyncedAt = existing?.lastSyncedAt
            )
            val id = cpProfileDao.upsert(entity)

            // Trigger sync immediately for the newly saved profile
            syncProfile(platform, handle)

            AppResult.Success(id)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to add profile"))
        }
    }

    private suspend fun fetchAndPersistProfile(
        platform: String,
        handle: String,
        existingId: Long,
        syncedAtMs: Long
    ): AppResult<Unit> {
        return when (platform.uppercase()) {
            CpProfileEntity.PLATFORM_CODECHEF -> {
                if (codeChefApiService == null) return AppResult.Success(Unit)
                try {
                    val response = codeChefApiService.getUserProfile(handle)
                    val mappedProfile = CodeChefMapper.mapProfile(
                        dto = response,
                        fallbackHandle = handle,
                        existingId = existingId,
                        syncedAtMs = syncedAtMs
                    )
                    if (mappedProfile != null) {
                        cpProfileDao.upsert(mappedProfile)
                    }

                    val mappedContests = CodeChefMapper.mapContests(
                        dtos = response.ratingData,
                        profileId = existingId
                    )
                    if (mappedContests.isNotEmpty()) {
                        cpContestDao.upsertContests(mappedContests)
                    }
                    AppResult.Success(Unit)
                } catch (e: Exception) {
                    AppResult.Failure(AppError.NetworkError(e.message ?: "CodeChef API request failed"))
                }
            }

            CpProfileEntity.PLATFORM_CODEFORCES -> {
                if (codeforcesApiService == null) return AppResult.Success(Unit)
                try {
                    val userInfo = codeforcesApiService.getUserInfo(handle)
                    val userRating = codeforcesApiService.getUserRating(handle)

                    val userDto = userInfo.result?.firstOrNull()
                    val mappedProfile = CodeforcesMapper.mapProfile(
                        dto = userDto,
                        fallbackHandle = handle,
                        existingId = existingId,
                        syncedAtMs = syncedAtMs
                    )?.copy(
                        highestRating = userDto?.maxRating,
                        rank = userDto?.rank,
                        contestCount = userRating.result?.size
                    )

                    if (mappedProfile != null) {
                        cpProfileDao.upsert(mappedProfile)
                    }

                    val mappedContests = CodeforcesMapper.mapContests(
                        dtos = userRating.result,
                        profileId = existingId
                    )
                    if (mappedContests.isNotEmpty()) {
                        cpContestDao.upsertContests(mappedContests)
                    }
                    AppResult.Success(Unit)
                } catch (e: Exception) {
                    AppResult.Failure(AppError.NetworkError(e.message ?: "Codeforces API request failed"))
                }
            }

            else -> AppResult.Failure(AppError.ValidationError("Unsupported platform $platform"))
        }
    }

    private fun CpProfileEntity.toDomain() = CpProfile(
        id = id,
        platform = platform,
        handle = handle,
        currentRating = currentRating,
        highestRating = highestRating,
        rank = rank,
        problemsSolved = problemsSolved,
        contestCount = contestCount,
        lastSyncedAt = lastSyncedAt
    )

    private fun CpContestEntity.toDomain() = CpContest(
        id = id,
        profileId = profileId,
        contestName = contestName,
        contestDate = contestDate,
        rank = rank,
        ratingChange = ratingChange,
        problemsSolved = problemsSolved
    )

    private fun CpReflectionEntity.toDomain() = CpReflection(
        id = id,
        contestId = contestId,
        wentWrong = wentWrong,
        toRevise = toRevise,
        selfRating = selfRating
    )

    private fun CpReflection.toEntity() = CpReflectionEntity(
        id = id,
        contestId = contestId,
        wentWrong = wentWrong,
        toRevise = toRevise,
        selfRating = selfRating
    )
}
