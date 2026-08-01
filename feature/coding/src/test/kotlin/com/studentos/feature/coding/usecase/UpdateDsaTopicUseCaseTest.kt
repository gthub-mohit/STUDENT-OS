package com.studentos.feature.coding.usecase

import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import com.studentos.feature.coding.domain.repository.DsaRepository
import com.studentos.feature.coding.domain.usecase.UpdateDsaTopicUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateDsaTopicUseCaseTest {

    private class FakeAppEventBus : AppEventBus {
        val emittedEvents = mutableListOf<AppEvent>()
        private val _events = MutableSharedFlow<AppEvent>(replay = 10)
        override val events: SharedFlow<AppEvent> = _events.asSharedFlow()

        override suspend fun emit(event: AppEvent) {
            emittedEvents.add(event)
            _events.emit(event)
        }
    }

    private class FakeDsaRepository : DsaRepository {
        var updatedTopic: DsaTopic? = null

        override fun getCategories(): Flow<List<DsaCategory>> = flowOf(emptyList())
        override fun getTopicsByCategory(categoryId: Long): Flow<List<DsaTopic>> = flowOf(emptyList())
        override suspend fun addCategory(name: String, sortOrder: Int): Long = 1L
        override suspend fun deleteCategory(id: Long) {}
        override suspend fun updateTopic(topic: DsaTopic) {
            updatedTopic = topic
        }
        override suspend fun addTopic(topic: DsaTopic): Long = 1L
    }

    private lateinit var repository: FakeDsaRepository
    private lateinit var eventBus: FakeAppEventBus
    private lateinit var useCase: UpdateDsaTopicUseCase

    @Before
    fun setUp() {
        repository = FakeDsaRepository()
        eventBus = FakeAppEventBus()
        useCase = UpdateDsaTopicUseCase(repository, eventBus)
    }

    @Test
    fun invoke_validTopic_updatesRepositoryAndEmitsEvent() = runTest {
        val topic = DsaTopic(id = 10L, categoryId = 1L, name = "B-Tree", confidenceLevel = 4, revisionStatus = DsaTopic.STATUS_REVISED)

        useCase(topic)

        assertNotNull(repository.updatedTopic)
        assertEquals(10L, repository.updatedTopic?.id)
        assertEquals(4, repository.updatedTopic?.confidenceLevel)

        assertEquals(1, eventBus.emittedEvents.size)
        val event = eventBus.emittedEvents[0]
        assertTrue(event is AppEvent.DsaTopicUpdated)
        assertEquals(10L, (event as AppEvent.DsaTopicUpdated).topicId)
    }

    @Test
    fun invoke_invalidConfidenceLevel_coercesBetween1And5() = runTest {
        val invalidHigh = DsaTopic(id = 1L, categoryId = 1L, name = "Topic", confidenceLevel = 10)
        useCase(invalidHigh)
        assertEquals(5, repository.updatedTopic?.confidenceLevel)

        val invalidLow = DsaTopic(id = 2L, categoryId = 1L, name = "Topic", confidenceLevel = 0)
        useCase(invalidLow)
        assertEquals(1, repository.updatedTopic?.confidenceLevel)
    }
}
