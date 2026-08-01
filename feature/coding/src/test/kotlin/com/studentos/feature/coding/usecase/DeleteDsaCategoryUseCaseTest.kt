package com.studentos.feature.coding.usecase

import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import com.studentos.feature.coding.domain.repository.DsaRepository
import com.studentos.feature.coding.domain.usecase.DeleteDsaCategoryUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DeleteDsaCategoryUseCaseTest {

    private class FakeDsaRepository : DsaRepository {
        var deletedId: Long? = null

        override fun getCategories(): Flow<List<DsaCategory>> = flowOf(emptyList())
        override fun getTopicsByCategory(categoryId: Long): Flow<List<DsaTopic>> = flowOf(emptyList())
        override suspend fun addCategory(name: String, sortOrder: Int): Long = 1L
        override suspend fun deleteCategory(id: Long) {
            deletedId = id
        }
        override suspend fun updateTopic(topic: DsaTopic) {}
        override suspend fun addTopic(topic: DsaTopic): Long = 1L
    }

    private lateinit var repository: FakeDsaRepository
    private lateinit var useCase: DeleteDsaCategoryUseCase

    @Before
    fun setUp() {
        repository = FakeDsaRepository()
        useCase = DeleteDsaCategoryUseCase(repository)
    }

    @Test
    fun invoke_validId_callsDeleteCategoryOnRepository() = runTest {
        useCase(15L)
        assertEquals(15L, repository.deletedId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invoke_invalidId_throwsException() = runTest {
        useCase(0L)
    }
}
