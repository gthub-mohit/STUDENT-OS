package com.studentos.core.sync.mapper

import com.studentos.core.database.entity.CpProfileEntity
import com.studentos.core.sync.api.dto.CodeforcesContestDto
import com.studentos.core.sync.api.dto.CodeforcesProfileDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeforcesMapperTest {

    @Test
    fun mapProfile_normalMapping_returnsValidCpProfileEntity() {
        val dto = CodeforcesProfileDto(
            handle = "tourist",
            rating = 3800,
            maxRating = 3900,
            rank = "legendary grandmaster"
        )

        val profile = CodeforcesMapper.mapProfile(
            dto = dto,
            fallbackHandle = "tourist",
            existingId = 5L,
            syncedAtMs = 1700000000000L
        )

        assertNotNull(profile)
        assertEquals(5L, profile?.id)
        assertEquals(CpProfileEntity.PLATFORM_CODEFORCES, profile?.platform)
        assertEquals("tourist", profile?.handle)
        assertEquals(3800, profile?.currentRating)
        assertEquals(1700000000000L, profile?.lastSyncedAt)
    }

    @Test
    fun mapProfile_nullDto_usesFallbackHandle() {
        val profile = CodeforcesMapper.mapProfile(
            dto = null,
            fallbackHandle = "fallback_handle",
            existingId = 0L,
            syncedAtMs = 1700000000000L
        )

        assertNotNull(profile)
        assertEquals("fallback_handle", profile?.handle)
        assertNull(profile?.currentRating)
    }

    @Test
    fun mapProfile_blankHandleAndBlankFallback_returnsNull() {
        val dto = CodeforcesProfileDto(handle = "")
        val profile = CodeforcesMapper.mapProfile(dto = dto, fallbackHandle = "   ")
        assertNull(profile)
    }

    @Test
    fun mapProfile_negativeRating_setsCurrentRatingToNull() {
        val dto = CodeforcesProfileDto(handle = "test_user", rating = -50)
        val profile = CodeforcesMapper.mapProfile(dto = dto, fallbackHandle = "test_user")

        assertNotNull(profile)
        assertNull(profile?.currentRating)
    }

    @Test
    fun mapContests_normalMapping_calculatesRatingChangeAndDates() {
        val dtos = listOf(
            CodeforcesContestDto(
                contestId = 100L,
                contestName = "Codeforces Round #800",
                rank = 42,
                ratingUpdateTimeSeconds = 1650000000L,
                oldRating = 1500,
                newRating = 1600
            )
        )

        val contests = CodeforcesMapper.mapContests(dtos, profileId = 5L)

        assertEquals(1, contests.size)
        val contest = contests[0]
        assertEquals(5L, contest.profileId)
        assertEquals("Codeforces Round #800", contest.contestName)
        assertEquals(1650000000000L, contest.contestDate)
        assertEquals(42, contest.rank)
        assertEquals(100, contest.ratingChange) // 1600 - 1500
        assertNull(contest.problemsSolved)
    }

    @Test
    fun mapContests_emptyList_returnsEmptyList() {
        val contests = CodeforcesMapper.mapContests(emptyList(), profileId = 5L)
        assertTrue(contests.isEmpty())
    }

    @Test
    fun mapContests_nullList_returnsEmptyList() {
        val contests = CodeforcesMapper.mapContests(null, profileId = 5L)
        assertTrue(contests.isEmpty())
    }

    @Test
    fun mapContests_invalidProfileId_returnsEmptyList() {
        val dtos = listOf(
            CodeforcesContestDto(
                contestId = 100L,
                contestName = "Round #800",
                ratingUpdateTimeSeconds = 1650000000L
            )
        )
        val contests = CodeforcesMapper.mapContests(dtos, profileId = 0L)
        assertTrue(contests.isEmpty())
    }

    @Test
    fun mapContests_missingNameOrDate_filtersOutInvalidEntries() {
        val dtos = listOf(
            CodeforcesContestDto(contestId = 1L, contestName = null, ratingUpdateTimeSeconds = 1000L),
            CodeforcesContestDto(contestId = 2L, contestName = "Valid Name", ratingUpdateTimeSeconds = null),
            CodeforcesContestDto(contestId = 3L, contestName = "  ", ratingUpdateTimeSeconds = 1000L)
        )

        val contests = CodeforcesMapper.mapContests(dtos, profileId = 5L)
        assertTrue(contests.isEmpty())
    }
}
