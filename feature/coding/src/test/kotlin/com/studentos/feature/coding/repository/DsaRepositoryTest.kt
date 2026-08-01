package com.studentos.feature.coding.repository

import app.cash.turbine.test
import com.studentos.core.database.dao.DsaCategoryDao
import com.studentos.core.database.dao.DsaTopicDao
import com.studentos.core.database.entity.DsaCategoryEntity
import com.studentos.core.database.entity.DsaTopicEntity
import com.studentos.feature.coding.data.repository.DsaRepositoryImpl
import com.studentos.feature.coding.domain.model.DsaTopic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DsaRepositoryTest {

    private class FakeDsaCategoryDao : DsaCategoryDao {
        val categories = mutableListOf<DsaCategoryEntity>()
        private val categoriesFlow = MutableStateFlow<List<DsaCategoryEntity>>(emptyList())

        override suspend fun insert(category: DsaCategoryEntity): Long {
            val id = (categories.size + 1).toLong()
            val created = category.copy(id = id)
            categories.add(created)
            categoriesFlow.value = categories.toList()
            return id
        }

        override suspend fun update(category: DsaCategoryEntity) {
            val index = categories.indexOfFirst { it.id == category.id }
            if (index >= 0) {
                categories[index] = category
                categoriesFlow.value = categories.toList()
            }
        }

        override suspend fun deleteById(id: Long) {
            categories.removeAll { it.id == id }
            categoriesFlow.value = categories.toList()
        }

        override fun getAllCategories(): Flow<List<DsaCategoryEntity>> = categoriesFlow
        override fun getCategoryById(id: Long): Flow<DsaCategoryEntity?> = flowOf(categories.firstOrNull { it.id == id })
        override suspend fun getCategoryCount(): Int = categories.size
    }

    private class FakeDsaTopicDao : DsaTopicDao {
        val topics = mutableListOf<DsaTopicEntity>()
        private val topicsFlowMap = mutableMapOf<Long, MutableStateFlow<List<DsaTopicEntity>>>()

        private fun getOrCreateFlow(categoryId: Long): MutableStateFlow<List<DsaTopicEntity>> {
            return topicsFlowMap.getOrPut(categoryId) {
                MutableStateFlow(topics.filter { it.categoryId == categoryId })
            }
        }

        private fun notifyCategory(categoryId: Long) {
            getOrCreateFlow(categoryId).value = topics.filter { it.categoryId == categoryId }
        }

        override suspend fun insert(topic: DsaTopicEntity): Long {
            val id = (topics.size + 1).toLong()
            val created = topic.copy(id = id)
            topics.add(created)
            notifyCategory(topic.categoryId)
            return id
        }

        override suspend fun update(topic: DsaTopicEntity) {
            val index = topics.indexOfFirst { it.id == topic.id }
            if (index >= 0) {
                topics[index] = topic
                notifyCategory(topic.categoryId)
            }
        }

        override suspend fun updateMastery(
            id: Long,
            confidenceLevel: Int,
            revisionStatus: String,
            notes: String?,
            updatedAt: Long
        ) {
            val index = topics.indexOfFirst { it.id == id }
            if (index >= 0) {
                val current = topics[index]
                val updated = current.copy(
                    confidenceLevel = confidenceLevel,
                    revisionStatus = revisionStatus,
                    notes = notes,
                    updatedAt = updatedAt
                )
                topics[index] = updated
                notifyCategory(current.categoryId)
            }
        }

        override suspend fun deleteById(id: Long) {
            val topic = topics.firstOrNull { it.id == id }
            if (topic != null) {
                topics.remove(topic)
                notifyCategory(topic.categoryId)
            }
        }

        override fun getTopicsByCategory(categoryId: Long): Flow<List<DsaTopicEntity>> = getOrCreateFlow(categoryId)
        override fun getTopicsByRevisionStatus(status: String): Flow<List<DsaTopicEntity>> = flowOf(topics.filter { it.revisionStatus == status })
        override fun getTopicsFilteredBy(revisionStatus: String, confidenceLevel: Int): Flow<List<DsaTopicEntity>> = flowOf(topics.filter { it.revisionStatus == revisionStatus && it.confidenceLevel == confidenceLevel })
        override suspend fun getSuggestedTopic(): DsaTopicEntity? = topics.firstOrNull()
        override suspend fun getAllMastered(): Boolean = topics.all { it.confidenceLevel == 5 && it.revisionStatus == "REVISED" }
        override suspend fun getTopicCount(): Int = topics.size
    }

    private lateinit var categoryDao: FakeDsaCategoryDao
    private lateinit var topicDao: FakeDsaTopicDao
    private lateinit var repository: DsaRepositoryImpl

    @Before
    fun setUp() {
        categoryDao = FakeDsaCategoryDao()
        topicDao = FakeDsaTopicDao()
        repository = DsaRepositoryImpl(categoryDao, topicDao)
    }

    @Test
    fun addCategory_and_getCategories_mapsEntitiesToDomainCorrectly() = runTest {
        repository.getCategories().test {
            assertEquals(emptyList<Any>(), awaitItem())

            val id1 = repository.addCategory("Trees", 1)
            assertEquals(1L, id1)

            val categories = awaitItem()
            assertEquals(1, categories.size)
            assertEquals("Trees", categories[0].name)
            assertEquals(1, categories[0].sortOrder)
        }
    }

    @Test
    fun deleteCategory_removesCategoryFromFlow() = runTest {
        val id = repository.addCategory("Graphs", 2)
        repository.getCategories().test {
            val initial = awaitItem()
            assertEquals(1, initial.size)

            repository.deleteCategory(id)
            val updated = awaitItem()
            assertTrue(updated.isEmpty())
        }
    }

    @Test
    fun updateTopic_updatesMasteryFieldsInDao() = runTest {
        val topicId = topicDao.insert(
            DsaTopicEntity(categoryId = 1L, name = "Segment Tree", confidenceLevel = 2, revisionStatus = "NOT_STARTED")
        )

        repository.getTopicsByCategory(1L).test {
            val initial = awaitItem()
            assertEquals(2, initial[0].confidenceLevel)

            val updatedTopic = DsaTopic(
                id = topicId,
                categoryId = 1L,
                name = "Segment Tree",
                confidenceLevel = 4,
                revisionStatus = DsaTopic.STATUS_REVISED
            )
            repository.updateTopic(updatedTopic)

            val updatedList = awaitItem()
            assertEquals(4, updatedList[0].confidenceLevel)
            assertEquals(DsaTopic.STATUS_REVISED, updatedList[0].revisionStatus)
        }
    }
}
