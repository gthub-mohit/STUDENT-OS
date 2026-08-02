package com.studentos.feature.coding.data.repository

import com.studentos.core.database.dao.DsaCategoryDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.entity.DsaCategoryEntity
import com.studentos.core.database.entity.DsaTopicEntity
import com.studentos.core.events.AppError
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppResult
import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import com.studentos.feature.coding.domain.repository.DsaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DsaRepositoryImpl @Inject constructor(
    private val dsaCategoryDao: DsaCategoryDao,
    private val dsaTopicDao: DsaTopicDao,
    private val eventBus: AppEventBus
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

    override fun getAllTopics(): Flow<List<DsaTopic>> {
        return dsaTopicDao.getAllTopics().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRevisionQueue(nowEpochMs: Long): Flow<List<DsaTopic>> {
        return dsaTopicDao.getRevisionQueue(nowEpochMs).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addCategory(name: String, sortOrder: Int): Long = withContext(Dispatchers.IO) {
        val entity = DsaCategoryEntity(name = name, sortOrder = sortOrder)
        dsaCategoryDao.insert(entity)
    }

    override suspend fun deleteCategory(id: Long) = withContext(Dispatchers.IO) {
        dsaCategoryDao.deleteById(id)
    }

    override suspend fun updateTopic(topic: DsaTopic) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val nextRevision = calculateNextRevisionDate(topic.confidenceLevel, now)
        dsaTopicDao.updateMastery(
            id = topic.id,
            confidenceLevel = topic.confidenceLevel,
            revisionStatus = topic.revisionStatus,
            nextRevisionDate = nextRevision,
            notes = topic.notes,
            updatedAt = if (topic.updatedAt > 0) topic.updatedAt else now
        )
        eventBus.emit(AppEvent.DsaTopicUpdated(topic.id))
    }

    override suspend fun addTopic(topic: DsaTopic): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val nextRevision = calculateNextRevisionDate(topic.confidenceLevel, now)
        val entity = DsaTopicEntity(
            categoryId = topic.categoryId,
            name = topic.name,
            difficulty = topic.difficulty,
            confidenceLevel = topic.confidenceLevel,
            revisionStatus = topic.revisionStatus,
            nextRevisionDate = nextRevision,
            notes = topic.notes,
            updatedAt = now
        )
        val newId = dsaTopicDao.insert(entity)
        eventBus.emit(AppEvent.DsaTopicUpdated(newId))
        newId
    }

    override suspend fun updateTopicConfidence(topicId: Long, confidence: Int): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = dsaTopicDao.getAllTopics().firstOrNull()?.find { it.id == topicId }
                ?: return@withContext AppResult.Failure(AppError.ValidationError("Topic not found"))

            val validConfidence = confidence.coerceIn(1, 5)
            val now = System.currentTimeMillis()
            val nextRevision = calculateNextRevisionDate(validConfidence, now)

            val newStatus = when {
                validConfidence >= 4 -> DsaTopic.STATUS_REVISED
                validConfidence >= 2 -> DsaTopic.STATUS_IN_PROGRESS
                else -> DsaTopic.STATUS_NOT_STARTED
            }

            dsaTopicDao.updateMastery(
                id = topicId,
                confidenceLevel = validConfidence,
                revisionStatus = newStatus,
                nextRevisionDate = nextRevision,
                notes = existing.notes,
                updatedAt = now
            )
            eventBus.emit(AppEvent.DsaTopicUpdated(topicId))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to update confidence"))
        }
    }

    override suspend fun completeRevision(topicId: Long): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = dsaTopicDao.getAllTopics().firstOrNull()?.find { it.id == topicId }
                ?: return@withContext AppResult.Failure(AppError.ValidationError("Topic not found"))

            val now = System.currentTimeMillis()
            val newConfidence = (existing.confidenceLevel + 1).coerceAtMost(5)
            val nextRevision = calculateNextRevisionDate(newConfidence, now)

            dsaTopicDao.updateMastery(
                id = topicId,
                confidenceLevel = newConfidence,
                revisionStatus = DsaTopic.STATUS_REVISED,
                nextRevisionDate = nextRevision,
                notes = existing.notes,
                updatedAt = now
            )
            eventBus.emit(AppEvent.DsaTopicUpdated(topicId))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to complete revision"))
        }
    }

    private fun calculateNextRevisionDate(confidenceLevel: Int, lastRevisedMs: Long): Long {
        val days = when (confidenceLevel.coerceIn(1, 5)) {
            1 -> 1
            2 -> 3
            3 -> 7
            4 -> 14
            5 -> 30
            else -> 7
        }
        return lastRevisedMs + (days * 86400000L)
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
        difficulty = difficulty,
        confidenceLevel = confidenceLevel,
        revisionStatus = revisionStatus,
        nextRevisionDate = nextRevisionDate,
        notes = notes,
        updatedAt = updatedAt
    )
}
