package com.studentos.feature.intelligence.domain.model.fact

data class ContestItemFact(
    val id: Long,
    val name: String,
    val dateEpochMs: Long,
    val startsInHours: Long
)

data class DsaTopicFact(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val confidenceLevel: Int
)

data class CodingFact(
    val lastSyncedAtEpochMs: Long? = null,
    val upcomingContests: List<ContestItemFact> = emptyList(),
    val suggestedDsaTopic: DsaTopicFact? = null
)
