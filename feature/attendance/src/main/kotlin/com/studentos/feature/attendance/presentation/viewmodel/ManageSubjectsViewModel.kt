package com.studentos.feature.attendance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import com.studentos.feature.attendance.domain.usecase.ArchiveSubjectUseCase
import com.studentos.feature.attendance.presentation.state.ManageSubjectsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageSubjectsViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val archiveSubjectUseCase: ArchiveSubjectUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManageSubjectsUiState>(ManageSubjectsUiState.Loading)
    val uiState: StateFlow<ManageSubjectsUiState> = _uiState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch(Dispatchers.IO) {
            subjectRepository.getAllSubjectsIncludingArchived().collect { allSubjects ->
                val active = allSubjects.filter { it.archivedAt == null }
                val archived = allSubjects.filter { it.archivedAt != null }
                _uiState.value = ManageSubjectsUiState.Success(
                    activeSubjects = active,
                    archivedSubjects = archived
                )
            }
        }
    }

    fun addSubject(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = subjectRepository.addSubject(name)
            if (result is AppResult.Failure) {
                _uiState.value = ManageSubjectsUiState.Error(result.reason.toString())
            }
        }
    }

    fun renameSubject(id: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = subjectRepository.renameSubject(id, newName)
            if (result is AppResult.Failure) {
                _uiState.value = ManageSubjectsUiState.Error(result.reason.toString())
            }
        }
    }

    fun archiveSubject(id: Long, confirmWithActiveSlots: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = archiveSubjectUseCase(id, confirmWithActiveSlots)
            if (result is AppResult.Failure) {
                _uiState.value = ManageSubjectsUiState.Error(result.reason.toString())
            }
        }
    }
}
