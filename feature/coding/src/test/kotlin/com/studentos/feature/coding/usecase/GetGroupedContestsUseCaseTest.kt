package com.studentos.feature.coding.usecase

import com.studentos.core.events.AppResult
import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.CpProfile
import com.studentos.feature.coding.domain.model.CpReflection
import com.studentos.feature.coding.domain.repository.CpRepository
import com.studentos.feature.coding.domain.usecase.GetGroupedContestsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetGroupedContestsUseCaseTest {

    private class FakeCpRepository(private val list: List<CpContest>) : CpRepository {
        override fun getProfiles(): Flow<List<CpProfile>> = flowOf(emptyList())
        override fun getContests(profileId: Long): Flow<List<CpContest>> = flowOf(emptyList())
        override fun getAllContests(): Flow<List<CpContest>> = flowOf(list)
        override fun getReflection(contestId: Long): Flow<CpReflection?> = flowOf(null)
        override suspend fun saveReflection(reflection: CpReflection) {}
        override suspend fun syncProfiles(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun syncProfile(platform: String, handle: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun addOrUpdateProfile(platform: String, handle: String): AppResult<Long> = AppResult.Success(1L)
    }

    @Test
    fun groupContests_categorizesContestsCorrectly() = runBlocking {
        val now = System.currentTimeMillis()
        val upcoming = CpContest(id = 1L, profileId = 1L, contestName = "Weekly 100", contestDate = now + 86400000L)
        val ongoing = CpContest(id = 2L, profileId = 1L, contestName = "Ongoing 200", contestDate = now)
        val completed = CpContest(id = 3L, profileId = 1L, contestName = "Past 300", contestDate = now - 86400000L)

        val repo = FakeCpRepository(listOf(upcoming, ongoing, completed))
        val useCase = GetGroupedContestsUseCase(repo)

        val result = useCase.invoke(nowEpochMs = now).first()
        assertEquals(1, result.upcoming.size)
        assertEquals("Weekly 100", result.upcoming[0].contestName)

        assertEquals(1, result.ongoing.size)
        assertEquals("Ongoing 200", result.ongoing[0].contestName)

        assertEquals(1, result.completed.size)
        assertEquals("Past 300", result.completed[0].contestName)
    }
}
