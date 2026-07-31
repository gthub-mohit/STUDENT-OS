package com.studentos.core.sync.mapper

import com.studentos.core.database.entity.CpContestEntity
import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.sync.api.dto.CodeforcesContestDto
import com.studentos.core.sync.api.dto.CodeforcesProfileDto

object CodeforcesMapper {

    fun mapProfile(
        dto: CodeforcesProfileDto?,
        fallbackHandle: String,
        existingId: Long = 0L,
        syncedAtMs: Long = System.currentTimeMillis()
    ): CpProfileEntity? {
        val handle = dto?.handle?.takeIf { it.isNotBlank() }
            ?: fallbackHandle.takeIf { it.isNotBlank() }
            ?: return null

        return CpProfileEntity(
            id = existingId,
            platform = CpProfileEntity.PLATFORM_CODEFORCES,
            handle = handle,
            currentRating = dto?.rating?.takeIf { it >= 0 },
            lastSyncedAt = syncedAtMs
        )
    }

    fun mapContests(
        dtos: List<CodeforcesContestDto>?,
        profileId: Long
    ): List<CpContestEntity> {
        if (dtos.isNullOrEmpty() || profileId <= 0L) return emptyList()

        return dtos.mapNotNull { dto ->
            val name = dto.contestName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val dateSeconds = dto.ratingUpdateTimeSeconds?.takeIf { it > 0 } ?: return@mapNotNull null
            val contestDateMs = dateSeconds * 1000L

            val oldR = dto.oldRating
            val newR = dto.newRating
            val ratingChange = if (oldR != null && newR != null) newR - oldR else null

            CpContestEntity(
                profileId = profileId,
                contestName = name,
                contestDate = contestDateMs,
                rank = dto.rank?.takeIf { it > 0 },
                ratingChange = ratingChange,
                problemsSolved = null
            )
        }
    }
}
