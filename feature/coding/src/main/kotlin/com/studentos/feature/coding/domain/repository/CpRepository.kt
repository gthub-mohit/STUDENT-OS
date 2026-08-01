package com.studentos.feature.coding.domain.repository

import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.CpProfile
import com.studentos.feature.coding.domain.model.CpReflection
import kotlinx.coroutines.flow.Flow

interface CpRepository {
    fun getProfiles(): Flow<List<CpProfile>>
    fun getContests(profileId: Long): Flow<List<CpContest>>
    fun getAllContests(): Flow<List<CpContest>>
    fun getReflection(contestId: Long): Flow<CpReflection?>
    suspend fun saveReflection(reflection: CpReflection)
}
