package com.studentos.feature.coding.data.repository

import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.CpProfileDao
import com.studentos.core.database.dao.CpReflectionDao
import com.studentos.core.database.entity.CpContestEntity
import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.database.entity.CpReflectionEntity
import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.CpProfile
import com.studentos.feature.coding.domain.model.CpReflection
import com.studentos.feature.coding.domain.repository.CpRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class CpRepositoryImpl @Inject constructor(
    private val cpProfileDao: CpProfileDao,
    private val cpContestDao: CpContestDao,
    private val cpReflectionDao: CpReflectionDao
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

    private fun CpProfileEntity.toDomain() = CpProfile(
        id = id,
        platform = platform,
        handle = handle,
        currentRating = currentRating,
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
