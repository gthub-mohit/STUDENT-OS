package com.studentos.feature.assignments.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.usecase.UpdateAssignmentStatusUseCase
import com.studentos.feature.assignments.presentation.state.AssignmentDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssignmentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AssignmentRepository,
    private val updateAssignmentStatusUseCase: UpdateAssignmentStatusUseCase,
    private val subjectDao: SubjectDao
) : ViewModel() {

    private val assignmentId: Long = savedStateHandle.get<Any>("id")?.toString()?.toLongOrNull() ?: 0L

    private val _showDeleteConfirmation = MutableStateFlow(false)
    private val _uiState = MutableStateFlow<AssignmentDetailUiState>(AssignmentDetailUiState.Loading)
    val uiState: StateFlow<AssignmentDetailUiState> = _uiState.asStateFlow()

    init {
        observeAssignment()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAssignment() {
        if (assignmentId <= 0L) {
            _uiState.value = AssignmentDetailUiState.Error("Invalid assignment ID")
            return
        }

        combine(
            repository.getAssignmentById(assignmentId),
            subjectDao.getAllSubjectsIncludingArchived(),
            _showDeleteConfirmation
        ) { assignment, subjects, showDelete ->
            if (assignment == null) {
                if (_uiState.value is AssignmentDetailUiState.Deleted) {
                    AssignmentDetailUiState.Deleted
                } else {
                    AssignmentDetailUiState.Error("Assignment not found")
                }
            } else {
                val subjectName = subjects.find { it.id == assignment.subjectId }?.name ?: "Subject"
                AssignmentDetailUiState.Success(
                    assignment = assignment,
                    subjectName = subjectName,
                    showDeleteConfirmation = showDelete
                )
            }
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun cycleStatus() {
        val currentState = _uiState.value as? AssignmentDetailUiState.Success ?: return
        val currentAssignment = currentState.assignment
        val nextStatus = when (currentAssignment.status) {
            AssignmentEntity.STATUS_PENDING -> AssignmentEntity.STATUS_IN_PROGRESS
            AssignmentEntity.STATUS_IN_PROGRESS -> AssignmentEntity.STATUS_SUBMITTED
            AssignmentEntity.STATUS_SUBMITTED -> AssignmentEntity.STATUS_COMPLETED
            AssignmentEntity.STATUS_COMPLETED -> AssignmentEntity.STATUS_IN_PROGRESS
            else -> AssignmentEntity.STATUS_IN_PROGRESS
        }
        viewModelScope.launch {
            updateAssignmentStatusUseCase(assignmentId, nextStatus)
        }
    }

    fun requestDelete() {
        val currentState = _uiState.value as? AssignmentDetailUiState.Success ?: return
        val status = currentState.assignment.status
        if (status == AssignmentEntity.STATUS_PENDING || status == AssignmentEntity.STATUS_IN_PROGRESS) {
            _showDeleteConfirmation.value = true
        } else {
            confirmDelete()
        }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            repository.deleteAssignment(assignmentId)
            _uiState.value = AssignmentDetailUiState.Deleted
        }
    }

    fun dismissDeleteDialog() {
        _showDeleteConfirmation.value = false
    }

    fun addAttachment(sourceUriString: String) {
        viewModelScope.launch {
            repository.attachFile(assignmentId, sourceUriString)
        }
    }

    fun updateDeadline(newDeadline: Long) {
        if (newDeadline <= 0L) return
        viewModelScope.launch {
            repository.updateDeadline(assignmentId, newDeadline)
        }
    }

    fun removeAttachment() {
        viewModelScope.launch {
            repository.setAttachment(assignmentId, null)
        }
    }
}
