package com.studentos.feature.attendance.presentation

import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import com.studentos.feature.attendance.domain.usecase.ArchiveSubjectUseCase
import com.studentos.feature.attendance.presentation.state.ManageSubjectsUiState
import com.studentos.feature.attendance.presentation.viewmodel.ManageSubjectsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageSubjectsViewModelTest {

    private class FakeSubjectRepository : SubjectRepository {
        val subjectsList = mutableListOf<SubjectEntity>(
            SubjectEntity(id = 1, name = "Mathematics", archivedAt = null),
            SubjectEntity(id = 2, name = "History", archivedAt = System.currentTimeMillis())
        )
        val subjectsFlow = MutableStateFlow<List<SubjectEntity>>(subjectsList.toList())

        override fun getActiveSubjects(): Flow<List<SubjectEntity>> = error("Not needed")

        override fun getAllSubjectsIncludingArchived(): Flow<List<SubjectEntity>> = subjectsFlow

        override fun getSubjectById(id: Long): Flow<SubjectEntity?> = error("Not needed")

        override suspend fun addSubject(name: String): AppResult<Long> {
            val newSubject = SubjectEntity(id = (subjectsList.size + 1).toLong(), name = name)
            subjectsList.add(newSubject)
            subjectsFlow.value = subjectsList.toList()
            return AppResult.Success(newSubject.id)
        }

        override suspend fun renameSubject(id: Long, newName: String): AppResult<Unit> {
            val index = subjectsList.indexOfFirst { it.id == id }
            if (index != -1) {
                subjectsList[index] = subjectsList[index].copy(name = newName)
                subjectsFlow.value = subjectsList.toList()
            }
            return AppResult.Success(Unit)
        }

        override suspend fun archiveSubject(id: Long): AppResult<Unit> {
            val index = subjectsList.indexOfFirst { it.id == id }
            if (index != -1) {
                subjectsList[index] = subjectsList[index].copy(archivedAt = System.currentTimeMillis())
                subjectsFlow.value = subjectsList.toList()
            }
            return AppResult.Success(Unit)
        }
    }

    private class FakeTimetableSlotDao : TimetableSlotDao {
        override suspend fun insert(slot: TimetableSlotEntity): Long = error("Not needed")
        override suspend fun insertAll(slots: List<TimetableSlotEntity>): List<Long> = error("Not needed")
        override suspend fun update(slot: TimetableSlotEntity) = error("Not needed")
        override suspend fun deleteById(id: Long) = error("Not needed")
        override suspend fun deleteAll() = error("Not needed")
        override fun getAllSlots(): Flow<List<TimetableSlotEntity>> = error("Not needed")
        override fun getSlotsForSubject(subjectId: Long): Flow<List<TimetableSlotEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
        override fun getSlotsForDay(dayOfWeek: Int, parity: String?): Flow<List<TimetableSlotEntity>> = error("Not needed")
        override suspend fun getActiveSlotsOnDate(epochMs: Long, parity: String?): List<TimetableSlotEntity> = error("Not needed")
    }

    @Test
    fun init_loadsActiveAndArchivedSubjectsSuccessfully() = runBlocking {
        val repo = FakeSubjectRepository()
        val dao = FakeTimetableSlotDao()
        val archiveUseCase = ArchiveSubjectUseCase(repo, dao)
        val viewModel = ManageSubjectsViewModel(repo, archiveUseCase)

        val state = viewModel.uiState.first { it is ManageSubjectsUiState.Success }
        assertTrue(state is ManageSubjectsUiState.Success)
        val success = state as ManageSubjectsUiState.Success
        assertEquals(1, success.activeSubjects.size)
        assertEquals("Mathematics", success.activeSubjects[0].name)
        assertEquals(1, success.archivedSubjects.size)
        assertEquals("History", success.archivedSubjects[0].name)
    }

    @Test
    fun addSubject_addsNewSubjectToActiveList() = runBlocking {
        val repo = FakeSubjectRepository()
        val dao = FakeTimetableSlotDao()
        val archiveUseCase = ArchiveSubjectUseCase(repo, dao)
        val viewModel = ManageSubjectsViewModel(repo, archiveUseCase)

        viewModel.uiState.first { it is ManageSubjectsUiState.Success }
        viewModel.addSubject("Physics")
        val state = viewModel.uiState.first {
            (it as? ManageSubjectsUiState.Success)?.activeSubjects?.size == 2
        } as ManageSubjectsUiState.Success
        assertEquals(2, state.activeSubjects.size)
        assertTrue(state.activeSubjects.any { it.name == "Physics" })
    }

    @Test
    fun renameSubject_updatesSubjectName() = runBlocking {
        val repo = FakeSubjectRepository()
        val dao = FakeTimetableSlotDao()
        val archiveUseCase = ArchiveSubjectUseCase(repo, dao)
        val viewModel = ManageSubjectsViewModel(repo, archiveUseCase)

        viewModel.uiState.first { it is ManageSubjectsUiState.Success }
        viewModel.renameSubject(1L, "Advanced Mathematics")
        val state = viewModel.uiState.first {
            (it as? ManageSubjectsUiState.Success)?.activeSubjects?.firstOrNull()?.name == "Advanced Mathematics"
        } as ManageSubjectsUiState.Success
        assertEquals("Advanced Mathematics", state.activeSubjects[0].name)
    }

    @Test
    fun archiveSubject_movesSubjectToArchivedList() = runBlocking {
        val repo = FakeSubjectRepository()
        val dao = FakeTimetableSlotDao()
        val archiveUseCase = ArchiveSubjectUseCase(repo, dao)
        val viewModel = ManageSubjectsViewModel(repo, archiveUseCase)

        viewModel.uiState.first { it is ManageSubjectsUiState.Success }
        viewModel.archiveSubject(1L, confirmWithActiveSlots = true)
        val state = viewModel.uiState.first {
            (it as? ManageSubjectsUiState.Success)?.activeSubjects?.isEmpty() == true
        } as ManageSubjectsUiState.Success
        assertEquals(0, state.activeSubjects.size)
        assertEquals(2, state.archivedSubjects.size)
    }
}
