package com.studentos.core.sync.mapper

import com.studentos.core.database.entity.CpContestEntity
import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.sync.api.dto.CodeChefContestDto
import com.studentos.core.sync.api.dto.CodeChefProfileResponseDto
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

object CodeChefMapper {

    fun mapProfile(
        dto: CodeChefProfileResponseDto?,
        fallbackHandle: String,
        existingId: Long = 0L,
        syncedAtMs: Long = System.currentTimeMillis()
    ): CpProfileEntity? {
        val handle = dto?.handle?.takeIf { it.isNotBlank() }
            ?: fallbackHandle.takeIf { it.isNotBlank() }
            ?: return null

        val rating = dto?.currentRating ?: dto?.rating

        return CpProfileEntity(
            id = existingId,
            platform = CpProfileEntity.PLATFORM_CODECHEF,
            handle = handle,
            currentRating = rating?.takeIf { it >= 0 },
            lastSyncedAt = syncedAtMs
        )
    }

    fun mapContests(
        dtos: List<CodeChefContestDto>?,
        profileId: Long
    ): List<CpContestEntity> {
        if (dtos.isNullOrEmpty() || profileId <= 0L) return emptyList()

        return dtos.mapNotNull { dto ->
            val name = dto.name?.takeIf { it.isNotBlank() }
                ?: dto.code?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            val contestDateMs = dto.getRatedDate
                ?: parseDateStringToEpochMs(dto.endDate)
                ?: return@mapNotNull null

            CpContestEntity(
                profileId = profileId,
                contestName = name,
                contestDate = contestDateMs,
                rank = dto.rank?.takeIf { it > 0 },
                ratingChange = dto.ratingChange,
                problemsSolved = dto.problemsSolved?.takeIf { it >= 0 }
            )
        }
    }

    private fun parseDateStringToEpochMs(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }
}
