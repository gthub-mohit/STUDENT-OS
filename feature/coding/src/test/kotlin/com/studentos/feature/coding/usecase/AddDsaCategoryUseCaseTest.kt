package com.studentos.feature.coding.usecase

import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import com.studentos.feature.coding.domain.repository.DsaRepository
import com.studentos.feature.coding.domain.usecase.AddDsaCategoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddDsaCategoryUseCaseTest {

    private class FakeDsaRepository : DsaRepository {
        var addedName: String? = null

        override fun getCategories(): Flow<List<DsaCategory>> = flowOf(emptyList())
        override fun getTopicsByCategory(categoryId: Long): Flow<List<DsaTopic>> = flowOf(emptyList())
        override fun getAllTopics(): Flow<List<DsaTopic>> = flowOf(emptyList())
        override fun getRevisionQueue(nowEpochMs: Long): Flow<List<DsaTopic>> = flowOf(emptyList())
        override suspend fun addCategory(name: String, sortOrder: Int): Long {
            addedName = name
            return 1L
        }
        override suspend fun deleteCategory(id: Long) {}
        override suspend fun updateTopic(topic: DsaTopic) {}
        override suspend fun addTopic(topic: DsaTopic): Long = 1L
        override suspend fun updateTopicConfidence(topicId: Long, confidence: Int): com.studentos.core.events.AppResult<Unit> = com.studentos.core.events.AppResult.Success(Unit)
        override suspend fun completeRevision(topicId: Long): com.studentos.core.events.AppResult<Unit> = com.studentos.core.events.AppResult.Success(Unit)
    }

    private lateinit var repository: FakeDsaRepository
    private lateinit var useCase: AddDsaCategoryUseCase

    @Before
    fun setUp() {
        repository = FakeDsaRepository()
        useCase = AddDsaCategoryUseCase(repository)
    }

    @Test
    fun invoke_validName_trimsAndCallsRepository() = runTest {
        val id = useCase("  Dynamic Programming  ")
        assertEquals(1L, id)
        assertEquals("Dynamic Programming", repository.addedName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invoke_blankName_throwsException() = runTest {
        useCase("   ")
    }
}
