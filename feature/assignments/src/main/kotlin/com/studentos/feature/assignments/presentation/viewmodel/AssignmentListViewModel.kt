package com.studentos.feature.assignments.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.AssignmentEntity
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

    private val _selectedFilter = MutableStateFlow(AssignmentFilter.TODAY)
    val selectedFilter: StateFlow<AssignmentFilter> = _selectedFilter.asStateFlow()

    private val _selectedType = MutableStateFlow<TaskType?>(null)
    val selectedType: StateFlow<TaskType?> = _selectedType.asStateFlow()

    private val _assignmentToDelete = MutableStateFlow<AssignmentEntity?>(null)

    private val _uiState = MutableStateFlow<AssignmentListUiState>(AssignmentListUiState.Loading)
    val uiState: StateFlow<AssignmentListUiState> = _uiState.asStateFlow()

    init {
        observeAssignments()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAssignments() {
        val filterAndTypeFlow = combine(_selectedFilter, _selectedType) { filter, type -> filter to type }
        val subjectsFlow = combine(
            subjectDao.getAllSubjectsIncludingArchived(),
            subjectDao.getActiveSubjects()
        ) { all, active -> all to active }

        combine(
            filterAndTypeFlow.flatMapLatest { (filter, type) ->
                getFilteredAssignmentsUseCase(filter, type)
            },
            getPrioritizedAssignmentsUseCase(),
            subjectsFlow,
            filterAndTypeFlow,
            _assignmentToDelete
        ) { filteredAssignments, prioritizedGroups, (allSubjects, activeSubjects), (filter, type), toDelete ->
            val map = allSubjects.associate { it.id to it.name }
            AssignmentListUiState.Success(
                assignments = filteredAssignments,
                prioritizedGroups = prioritizedGroups,
                subjectsMap = map,
                activeSubjects = activeSubjects,
                currentFilter = filter,
                currentTypeFilter = type,
                assignmentToDelete = toDelete
            )
        }.catch { error ->
            _uiState.value = AssignmentListUiState.Error(error.message ?: "Failed to load tasks")
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun selectFilter(filter: AssignmentFilter) {
        _selectedFilter.value = filter
    }

    fun selectType(type: TaskType?) {
        _selectedType.value = type
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
