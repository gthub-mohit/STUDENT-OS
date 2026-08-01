package com.studentos.feature.coding.data.repository

import com.studentos.core.database.dao.DsaCategoryDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.entity.DsaCategoryEntity
import com.studentos.core.database.entity.DsaTopicEntity
import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import com.studentos.feature.coding.domain.repository.DsaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DsaRepositoryImpl @Inject constructor(
    private val dsaCategoryDao: DsaCategoryDao,
    private val dsaTopicDao: DsaTopicDao
) : DsaRepository {

    override fun getCategories(): Flow<List<DsaCategory>> {
        return dsaCategoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTopicsByCategory(categoryId: Long): Flow<List<DsaTopic>> {
        return dsaTopicDao.getTopicsByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addCategory(name: String, sortOrder: Int): Long {
        val entity = DsaCategoryEntity(name = name, sortOrder = sortOrder)
        return dsaCategoryDao.insert(entity)
    }

    override suspend fun deleteCategory(id: Long) {
        dsaCategoryDao.deleteById(id)
    }

    override suspend fun updateTopic(topic: DsaTopic) {
        dsaTopicDao.updateMastery(
            id = topic.id,
            confidenceLevel = topic.confidenceLevel,
            revisionStatus = topic.revisionStatus,
            notes = topic.notes,
            updatedAt = if (topic.updatedAt > 0) topic.updatedAt else System.currentTimeMillis()
        )
    }

    override suspend fun addTopic(topic: DsaTopic): Long {
        val entity = DsaTopicEntity(
            categoryId = topic.categoryId,
            name = topic.name,
            confidenceLevel = topic.confidenceLevel,
            revisionStatus = topic.revisionStatus,
            notes = topic.notes,
            updatedAt = System.currentTimeMillis()
        )
        return dsaTopicDao.insert(entity)
    }

    private fun DsaCategoryEntity.toDomain() = DsaCategory(
        id = id,
        name = name,
        sortOrder = sortOrder
    )

    private fun DsaTopicEntity.toDomain() = DsaTopic(
        id = id,
        categoryId = categoryId,
        name = name,
        confidenceLevel = confidenceLevel,
        revisionStatus = revisionStatus,
        notes = notes,
        updatedAt = updatedAt
    )
}
