package com.studentos.feature.coding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.feature.coding.domain.model.DsaCategory
import com.studentos.feature.coding.domain.model.DsaTopic
import com.studentos.feature.coding.domain.repository.DsaRepository
import com.studentos.feature.coding.domain.usecase.AddDsaCategoryUseCase
import com.studentos.feature.coding.domain.usecase.DeleteDsaCategoryUseCase
import com.studentos.feature.coding.domain.usecase.UpdateDsaTopicUseCase
import com.studentos.feature.coding.presentation.state.CategoryWithTopics
import com.studentos.feature.coding.presentation.state.DsaKnowledgeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class KnowledgeTreeViewModel @Inject constructor(
    private val dsaRepository: DsaRepository,
    private val addDsaCategoryUseCase: AddDsaCategoryUseCase,
    private val deleteDsaCategoryUseCase: DeleteDsaCategoryUseCase,
    private val updateDsaTopicUseCase: UpdateDsaTopicUseCase
) : ViewModel() {

    private val expandedIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val dialogStateFlow = MutableStateFlow<Pair<Boolean, DsaCategory?>>(Pair(false, null))

    val uiState: StateFlow<DsaKnowledgeUiState> = combine(
        dsaRepository.getCategories().flatMapLatest { categories ->
            if (categories.isEmpty()) {
                flowOf(emptyList())
            } else {
                val topicFlows = categories.map { category ->
                    dsaRepository.getTopicsByCategory(category.id).map { topics ->
                        Pair(category, topics)
                    }
                }
                combine(topicFlows) { pairs -> pairs.toList() }
            }
        },
        expandedIdsFlow,
        dialogStateFlow
    ) { categoryTopicPairs, expandedIds, (showAddDialog, categoryToDelete) ->
        val categoriesWithTopics = categoryTopicPairs.map { (category, topics) ->
            val isExpanded = if (expandedIds.isEmpty()) true else expandedIds.contains(category.id)
            CategoryWithTopics(
                category = category,
                topics = topics,
                isExpanded = isExpanded
            )
        }

        DsaKnowledgeUiState(
            isLoading = false,
            categories = categoriesWithTopics,
            expandedCategoryIds = expandedIds,
            showAddCategoryDialog = showAddDialog,
            categoryToDelete = categoryToDelete
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DsaKnowledgeUiState(isLoading = true)
    )

    fun toggleCategoryExpansion(categoryId: Long) {
        expandedIdsFlow.update { current ->
            if (current.contains(categoryId)) {
                current - categoryId
            } else {
                current + categoryId
            }
        }
    }

    fun onAddCategoryClicked() {
        dialogStateFlow.update { Pair(true, null) }
    }

    fun onDismissAddCategoryDialog() {
        dialogStateFlow.update { Pair(false, it.second) }
    }

    fun onConfirmAddCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                addDsaCategoryUseCase(name.trim())
                dialogStateFlow.update { Pair(false, it.second) }
            } catch (_: Exception) {
            }
        }
    }

    fun onDeleteCategoryClicked(category: DsaCategory) {
        dialogStateFlow.update { Pair(it.first, category) }
    }

    fun onDismissDeleteCategoryDialog() {
        dialogStateFlow.update { Pair(it.first, null) }
    }

    fun onConfirmDeleteCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                deleteDsaCategoryUseCase(categoryId)
                dialogStateFlow.update { Pair(it.first, null) }
            } catch (_: Exception) {
            }
        }
    }

    fun onTopicConfidenceChanged(topic: DsaTopic, newConfidence: Int) {
        viewModelScope.launch {
            val updated = topic.copy(confidenceLevel = newConfidence.coerceIn(1, 5))
            updateDsaTopicUseCase(updated)
        }
    }

    fun onToggleTopicSolved(topic: DsaTopic) {
        viewModelScope.launch {
            val newStatus = if (topic.revisionStatus == DsaTopic.STATUS_REVISED) {
                DsaTopic.STATUS_NOT_STARTED
            } else {
                DsaTopic.STATUS_REVISED
            }
            val updated = topic.copy(revisionStatus = newStatus)
            updateDsaTopicUseCase(updated)
        }
    }
}
