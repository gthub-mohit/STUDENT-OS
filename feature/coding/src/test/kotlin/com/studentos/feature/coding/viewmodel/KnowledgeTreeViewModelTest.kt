package com.studentos.feature.coding.viewmodel

import app.cash.turbine.test
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import com.studentos.feature.coding.domain.repository.DsaRepository
import com.studentos.feature.coding.domain.usecase.AddDsaCategoryUseCase
import com.studentos.feature.coding.domain.usecase.DeleteDsaCategoryUseCase
import com.studentos.feature.coding.domain.usecase.UpdateDsaTopicUseCase
import com.studentos.feature.coding.presentation.viewmodel.KnowledgeTreeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgeTreeViewModelTest {

    private class FakeAppEventBus : AppEventBus {
        val emittedEvents = mutableListOf<AppEvent>()
        private val _events = MutableSharedFlow<AppEvent>(replay = 10)
        override val events: SharedFlow<AppEvent> = _events.asSharedFlow()

        override suspend fun emit(event: AppEvent) {
            emittedEvents.add(event)
            _events.emit(event)
        }
    }

    private class FakeDsaRepository(private val eventBus: AppEventBus) : DsaRepository {
        val categoriesFlow = MutableStateFlow<List<DsaCategory>>(emptyList())
        val topicsMap = mutableMapOf<Long, MutableStateFlow<List<DsaTopic>>>()

        private fun getTopicsFlow(categoryId: Long): MutableStateFlow<List<DsaTopic>> {
            return topicsMap.getOrPut(categoryId) { MutableStateFlow(emptyList()) }
        }

        override fun getCategories(): Flow<List<DsaCategory>> = categoriesFlow
        override fun getTopicsByCategory(categoryId: Long): Flow<List<DsaTopic>> = getTopicsFlow(categoryId)

        override suspend fun addCategory(name: String, sortOrder: Int): Long {
            val id = (categoriesFlow.value.size + 1).toLong()
            val created = DsaCategory(id = id, name = name, sortOrder = sortOrder)
            categoriesFlow.value = categoriesFlow.value + created
            return id
        }

        override suspend fun deleteCategory(id: Long) {
            categoriesFlow.value = categoriesFlow.value.filter { it.id != id }
            topicsMap.remove(id)
        }

        override suspend fun updateTopic(topic: DsaTopic) {
            val flow = getTopicsFlow(topic.categoryId)
            val list = flow.value.toMutableList()
            val idx = list.indexOfFirst { it.id == topic.id }
            if (idx >= 0) {
                list[idx] = topic
                flow.value = list
            }
        }

        override fun getAllTopics(): Flow<List<DsaTopic>> = flowOf(emptyList())
        override fun getRevisionQueue(nowEpochMs: Long): Flow<List<DsaTopic>> = flowOf(emptyList())

        override suspend fun addTopic(topic: DsaTopic): Long {
            val flow = getTopicsFlow(topic.categoryId)
            val id = (flow.value.size + 1).toLong()
            val created = topic.copy(id = id)
            flow.value = flow.value + created
            return id
        }

        override suspend fun updateTopicConfidence(topicId: Long, confidence: Int): com.studentos.core.events.AppResult<Unit> {
            eventBus.emit(AppEvent.DsaTopicUpdated(topicId))
            return com.studentos.core.events.AppResult.Success(Unit)
        }
        override suspend fun completeRevision(topicId: Long): com.studentos.core.events.AppResult<Unit> {
            eventBus.emit(AppEvent.DsaTopicUpdated(topicId))
            return com.studentos.core.events.AppResult.Success(Unit)
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeDsaRepository
    private lateinit var eventBus: FakeAppEventBus
    private lateinit var addCategoryUseCase: AddDsaCategoryUseCase
    private lateinit var deleteCategoryUseCase: DeleteDsaCategoryUseCase
    private lateinit var updateTopicUseCase: UpdateDsaTopicUseCase
    private lateinit var viewModel: KnowledgeTreeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        eventBus = FakeAppEventBus()
        repository = FakeDsaRepository(eventBus)
        addCategoryUseCase = AddDsaCategoryUseCase(repository)
        deleteCategoryUseCase = DeleteDsaCategoryUseCase(repository)
        updateTopicUseCase = UpdateDsaTopicUseCase(repository, eventBus)
        viewModel = KnowledgeTreeViewModel(repository, addCategoryUseCase, deleteCategoryUseCase, updateTopicUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_isLoading() {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun uiState_emptyCategories_emitsEmptyState() = runTest {
        viewModel.uiState.test {
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            assertTrue(state.categories.isEmpty())
        }
    }

    @Test
    fun uiState_populatedCategoriesAndTopics_emitsTreeData() = runTest {
        val category = DsaCategory(id = 1L, name = "Trees")
        val topic = DsaTopic(id = 10L, categoryId = 1L, name = "Binary Tree", confidenceLevel = 3)

        viewModel.uiState.test {
            awaitItem() // Loading state

            repository.categoriesFlow.value = listOf(category)
            repository.topicsMap[1L] = MutableStateFlow(listOf(topic))
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertFalse(state.isEmpty)
            assertEquals(1, state.categories.size)
            assertEquals("Trees", state.categories[0].category.name)
            assertEquals(1, state.categories[0].topics.size)
            assertEquals("Binary Tree", state.categories[0].topics[0].name)
        }
    }

    @Test
    fun toggleCategoryExpansion_updatesExpandedIds() = runTest {
        val category = DsaCategory(id = 1L, name = "Trees")
        viewModel.uiState.test {
            awaitItem()
            repository.categoriesFlow.value = listOf(category)
            repository.topicsMap[1L] = MutableStateFlow(emptyList())
            testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() // Populated state

            viewModel.toggleCategoryExpansion(1L)
            testDispatcher.scheduler.advanceUntilIdle()

            val state = awaitItem()
            assertTrue(state.expandedCategoryIds.contains(1L))
        }
    }

    @Test
    fun onConfirmAddCategory_invokesUseCaseAndUpdatesState() = runTest {
        viewModel.onConfirmAddCategory("Graphs")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.categoriesFlow.value.size)
        assertEquals("Graphs", repository.categoriesFlow.value[0].name)
    }

    @Test
    fun onConfirmDeleteCategory_removesCategoryFromRepository() = runTest {
        repository.categoriesFlow.value = listOf(DsaCategory(id = 1L, name = "Trees"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onConfirmDeleteCategory(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.categoriesFlow.value.isEmpty())
    }

    @Test
    fun onTopicConfidenceChanged_updatesTopicAndEmitsEvent() = runTest {
        val topic = DsaTopic(id = 10L, categoryId = 1L, name = "BST", confidenceLevel = 2)
        repository.categoriesFlow.value = listOf(DsaCategory(id = 1L, name = "Trees"))
        repository.topicsMap[1L] = MutableStateFlow(listOf(topic))

        viewModel.onTopicConfidenceChanged(topic, 5)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, eventBus.emittedEvents.size)
        assertTrue(eventBus.emittedEvents[0] is AppEvent.DsaTopicUpdated)
        assertEquals(10L, (eventBus.emittedEvents[0] as AppEvent.DsaTopicUpdated).topicId)
    }
}
