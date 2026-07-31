package com.studentos.core.sync.mapper

import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.sync.api.dto.CodeChefContestDto
import com.studentos.core.sync.api.dto.CodeChefProfileResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeChefMapperTest {

    @Test
    fun mapProfile_normalMapping_returnsValidCpProfileEntity() {
        val dto = CodeChefProfileResponseDto(
            status = "success",
            handle = "chef_user",
            currentRating = 1850
        )

        val profile = CodeChefMapper.mapProfile(
            dto = dto,
            fallbackHandle = "chef_user",
            existingId = 3L,
            syncedAtMs = 1700000000000L
        )

        assertNotNull(profile)
        assertEquals(3L, profile?.id)
        assertEquals(CpProfileEntity.PLATFORM_CODECHEF, profile?.platform)
        assertEquals("chef_user", profile?.handle)
        assertEquals(1850, profile?.currentRating)
        assertEquals(1700000000000L, profile?.lastSyncedAt)
    }

    @Test
    fun mapProfile_nullDto_usesFallbackHandle() {
        val profile = CodeChefMapper.mapProfile(
            dto = null,
            fallbackHandle = "fallback_chef",
            existingId = 0L
        )

        assertNotNull(profile)
        assertEquals("fallback_chef", profile?.handle)
        assertNull(profile?.currentRating)
    }

    @Test
    fun mapProfile_blankHandleAndFallback_returnsNull() {
        val dto = CodeChefProfileResponseDto(handle = "")
        val profile = CodeChefMapper.mapProfile(dto = dto, fallbackHandle = "")
        assertNull(profile)
    }

    @Test
    fun mapProfile_negativeRating_setsCurrentRatingToNull() {
        val dto = CodeChefProfileResponseDto(handle = "user", currentRating = -10)
        val profile = CodeChefMapper.mapProfile(dto = dto, fallbackHandle = "user")

        assertNotNull(profile)
        assertNull(profile?.currentRating)
    }

    @Test
    fun mapContests_normalMapping_mapsAllFieldsCorrectly() {
        val dtos = listOf(
            CodeChefContestDto(
                code = "START100",
                name = "Starters 100",
                rank = 15,
                ratingChange = 45,
                getRatedDate = 1680000000000L,
                problemsSolved = 4
            )
        )

        val contests = CodeChefMapper.mapContests(dtos, profileId = 3L)

        assertEquals(1, contests.size)
        val contest = contests[0]
        assertEquals(3L, contest.profileId)
        assertEquals("Starters 100", contest.contestName)
        assertEquals(1680000000000L, contest.contestDate)
        assertEquals(15, contest.rank)
        assertEquals(45, contest.ratingChange)
        assertEquals(4, contest.problemsSolved)
    }

    @Test
    fun mapContests_isoDateString_parsesDateCorrectly() {
        val dtos = listOf(
            CodeChefContestDto(
                name = "Starters 101",
                endDate = "2024-05-15T18:00:00Z"
            )
        )

        val contests = CodeChefMapper.mapContests(dtos, profileId = 3L)
        assertEquals(1, contests.size)
        assertTrue((contests[0].contestDate) > 0L)
    }

    @Test
    fun mapContests_emptyList_returnsEmptyList() {
        val contests = CodeChefMapper.mapContests(emptyList(), profileId = 3L)
        assertTrue(contests.isEmpty())
    }

    @Test
    fun mapContests_nullList_returnsEmptyList() {
        val contests = CodeChefMapper.mapContests(null, profileId = 3L)
        assertTrue(contests.isEmpty())
    }
}
