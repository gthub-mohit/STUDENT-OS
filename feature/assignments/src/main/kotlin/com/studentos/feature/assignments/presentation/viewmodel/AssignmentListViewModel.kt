package com.studentos.feature.assignments.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.feature.assignments.domain.model.AssignmentFilter
import com.studentos.feature.assignments.domain.model.TaskType
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.usecase.CreateAssignmentUseCase
import com.studentos.feature.assignments.domain.usecase.GetFilteredAssignmentsUseCase
import com.studentos.feature.assignments.domain.usecase.GetPrioritizedAssignmentsUseCase
import com.studentos.feature.assignments.domain.usecase.UpdateAssignmentStatusUseCase
import com.studentos.feature.assignments.presentation.state.AssignmentListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssignmentListViewModel @Inject constructor(
    private val getFilteredAssignmentsUseCase: GetFilteredAssignmentsUseCase,
    private val getPrioritizedAssignmentsUseCase: GetPrioritizedAssignmentsUseCase,
    private val updateAssignmentStatusUseCase: UpdateAssignmentStatusUseCase,
    private val createAssignmentUseCase: CreateAssignmentUseCase,
    private val repository: AssignmentRepository,
    private val subjectDao: SubjectDao
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(AssignmentFilter.ALL)
    val selectedFilter: StateFlow<AssignmentFilter> = _selectedFilter.asStateFlow()

    private val _selectedType = MutableStateFlow<TaskType?>(null)
    val selectedType: StateFlow<TaskType?> = _selectedType.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    private val _selectedDeadline = MutableStateFlow<AssignmentFilter?>(null)
    val selectedDeadline: StateFlow<AssignmentFilter?> = _selectedDeadline.asStateFlow()

    private val _assignmentToDelete = MutableStateFlow<AssignmentEntity?>(null)

    private val _uiState = MutableStateFlow<AssignmentListUiState>(AssignmentListUiState.Loading)
    val uiState: StateFlow<AssignmentListUiState> = _uiState.asStateFlow()

    init {
        observeAssignments()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAssignments() {
        val filterCriteriaFlow = combine(
            _selectedFilter,
            _selectedType,
            _selectedStatus,
            _selectedDeadline
        ) { filter, type, status, deadline ->
            FilterCriteria(filter, type, status, deadline)
        }

        val filteredAssignmentsFlow = filterCriteriaFlow.flatMapLatest { criteria ->
            getFilteredAssignmentsUseCase(
                filter = criteria.filter,
                taskType = criteria.type,
                statusFilter = criteria.status,
                deadlineFilter = criteria.deadline
            )
        }

        val prioritizedGroupsFlow = filterCriteriaFlow.flatMapLatest { criteria ->
            getPrioritizedAssignmentsUseCase(
                taskType = criteria.type,
                statusFilter = criteria.status
            )
        }

        val metaDataFlow = combine(
            subjectDao.getAllSubjectsIncludingArchived(),
            subjectDao.getActiveSubjects(),
            repository.getAllAssignments(),
            filterCriteriaFlow
        ) { allSubjects, activeSubjects, allDbAssignments, criteria ->
            MetaData(
                subjectsMap = allSubjects.associate { it.id to it.name },
                activeSubjects = activeSubjects,
                totalCountInDb = allDbAssignments.size,
                criteria = criteria
            )
        }

        combine(
            filteredAssignmentsFlow,
            prioritizedGroupsFlow,
            metaDataFlow,
            _assignmentToDelete
        ) { filteredAssignments, prioritizedGroups, meta, toDelete ->
            AssignmentListUiState.Success(
                assignments = filteredAssignments,
                prioritizedGroups = prioritizedGroups,
                totalCountInDb = meta.totalCountInDb,
                subjectsMap = meta.subjectsMap,
                activeSubjects = meta.activeSubjects,
                currentFilter = meta.criteria.filter,
                currentTypeFilter = meta.criteria.type,
                currentStatusFilter = meta.criteria.status,
                currentDeadlineFilter = meta.criteria.deadline,
                assignmentToDelete = toDelete
            )
        }.catch { error ->
            _uiState.value = AssignmentListUiState.Error(error.message ?: "Failed to load tasks")
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    private data class MetaData(
        val subjectsMap: Map<Long, String>,
        val activeSubjects: List<SubjectEntity>,
        val totalCountInDb: Int,
        val criteria: FilterCriteria
    )

    private data class FilterCriteria(
        val filter: AssignmentFilter,
        val type: TaskType?,
        val status: String?,
        val deadline: AssignmentFilter?
    )

    fun selectFilter(filter: AssignmentFilter) {
        _selectedFilter.value = filter
    }

    fun selectType(type: TaskType?) {
        _selectedType.value = type
    }

    fun applyFilters(type: TaskType?, status: String?, deadline: AssignmentFilter?) {
        _selectedType.value = type
        _selectedStatus.value = status
        _selectedDeadline.value = deadline
        _selectedFilter.value = deadline ?: AssignmentFilter.ALL
    }

    fun clearAllFilters() {
        _selectedType.value = null
        _selectedStatus.value = null
        _selectedDeadline.value = null
        _selectedFilter.value = AssignmentFilter.ALL
    }

    fun clearTypeFilter() {
        _selectedType.value = null
    }

    fun clearStatusFilter() {
        _selectedStatus.value = null
    }

    fun clearDeadlineFilter() {
        _selectedDeadline.value = null
        _selectedFilter.value = AssignmentFilter.ALL
    }

    fun cycleStatus(assignment: AssignmentEntity) {
        viewModelScope.launch {
            val nextStatus = when (assignment.status) {
                AssignmentEntity.STATUS_PENDING -> AssignmentEntity.STATUS_IN_PROGRESS
                AssignmentEntity.STATUS_IN_PROGRESS -> AssignmentEntity.STATUS_SUBMITTED
                AssignmentEntity.STATUS_SUBMITTED -> AssignmentEntity.STATUS_COMPLETED
                AssignmentEntity.STATUS_COMPLETED -> AssignmentEntity.STATUS_IN_PROGRESS
                else -> AssignmentEntity.STATUS_IN_PROGRESS
            }
            updateAssignmentStatusUseCase(assignment.id, nextStatus)
        }
    }

    fun requestDelete(assignment: AssignmentEntity) {
        if (assignment.status == AssignmentEntity.STATUS_PENDING || assignment.status == AssignmentEntity.STATUS_IN_PROGRESS) {
            _assignmentToDelete.value = assignment
        } else {
            confirmDeleteAssignment(assignment.id)
        }
    }

    fun confirmDelete() {
        val target = _assignmentToDelete.value ?: return
        confirmDeleteAssignment(target.id)
        _assignmentToDelete.value = null
    }

    fun dismissDeleteDialog() {
        _assignmentToDelete.value = null
    }

    private fun confirmDeleteAssignment(id: Long) {
        viewModelScope.launch {
            repository.deleteAssignment(id)
        }
    }

    fun createAssignment(
        subjectId: Long,
        title: String,
        description: String?,
        deadline: Long,
        priority: String,
        taskType: TaskType = TaskType.ASSIGNMENT
    ) {
        viewModelScope.launch {
            val assignment = AssignmentEntity(
                subjectId = subjectId,
                title = title,
                description = description,
                deadline = deadline,
                priority = priority,
                status = AssignmentEntity.STATUS_PENDING,
                createdAt = System.currentTimeMillis(),
                taskType = taskType.name
            )
            createAssignmentUseCase(assignment)
        }
    }
}
