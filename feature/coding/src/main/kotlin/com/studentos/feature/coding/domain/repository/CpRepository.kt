package com.studentos.feature.coding.domain.repository

import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.CpProfile
import kotlinx.coroutines.flow.Flow

interface CpRepository {
    fun getProfiles(): Flow<List<CpProfile>>
    fun getContests(profileId: Long): Flow<List<CpContest>>
    fun getAllContests(): Flow<List<CpContest>>
}
