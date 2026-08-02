package com.studentos.feature.coding.domain.repository

import com.studentos.core.events.AppResult
import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import kotlinx.coroutines.flow.Flow

interface DsaRepository {
    fun getCategories(): Flow<List<DsaCategory>>
    fun getTopicsByCategory(categoryId: Long): Flow<List<DsaTopic>>
    fun getAllTopics(): Flow<List<DsaTopic>>
    fun getRevisionQueue(nowEpochMs: Long = System.currentTimeMillis()): Flow<List<DsaTopic>>
    suspend fun addCategory(name: String, sortOrder: Int): Long
    suspend fun deleteCategory(id: Long)
    suspend fun updateTopic(topic: DsaTopic)
    suspend fun addTopic(topic: DsaTopic): Long
    suspend fun updateTopicConfidence(topicId: Long, confidence: Int): AppResult<Unit>
    suspend fun completeRevision(topicId: Long): AppResult<Unit>
}
