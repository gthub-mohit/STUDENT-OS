package com.studentos.feature.coding.domain.repository

import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import kotlinx.coroutines.flow.Flow

interface DsaRepository {
    fun getCategories(): Flow<List<DsaCategory>>
    fun getTopicsByCategory(categoryId: Long): Flow<List<DsaTopic>>
    suspend fun addCategory(name: String, sortOrder: Int = 0): Long
    suspend fun deleteCategory(id: Long)
    suspend fun updateTopic(topic: DsaTopic)
    suspend fun addTopic(topic: DsaTopic): Long
}
