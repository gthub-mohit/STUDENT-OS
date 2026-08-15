package com.studentos.feature.attendance.presentation

import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import com.studentos.feature.attendance.presentation.state.EditTimetableUiState
import com.studentos.feature.attendance.presentation.viewmodel.EditTimetableViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditTimetableViewModelTest {

    private class FakeTimetableSlotDao : TimetableSlotDao {
        val slotsList = mutableListOf<TimetableSlotEntity>(
            TimetableSlotEntity(
                id = 1,
                subjectId = 10,
                dayOfWeek = 1,
                startTime = "09:00",
                endTime = "10:00",
                validFrom = System.currentTimeMillis()
            )
        )
        val slotsFlow = MutableStateFlow<List<TimetableSlotEntity>>(slotsList)

        override suspend fun insert(slot: TimetableSlotEntity): Long {
            val newSlot = slot.copy(id = (slotsList.size + 1).toLong())
            slotsList.add(newSlot)
            slotsFlow.value = slotsList.toList()
            return newSlot.id
        }

        override suspend fun insertAll(slots: List<TimetableSlotEntity>): List<Long> {
            return slots.map { insert(it) }
        }

        override suspend fun update(slot: TimetableSlotEntity) {
            val index = slotsList.indexOfFirst { it.id == slot.id }
            if (index != -1) {
                slotsList[index] = slot
                slotsFlow.value = slotsList.toList()
            }
        }

        override suspend fun deleteById(id: Long) {
            slotsList.removeAll { it.id == id }
            slotsFlow.value = slotsList.toList()
        }

        override suspend fun deleteAll() {
            slotsList.clear()
            slotsFlow.value = emptyList()
        }

        override fun getAllSlots(): Flow<List<TimetableSlotEntity>> = slotsFlow

        override fun getSlotsForSubject(subjectId: Long): Flow<List<TimetableSlotEntity>> = error("Not needed")
        override fun getSlotsForDay(dayOfWeek: Int, parity: String?): Flow<List<TimetableSlotEntity>> = error("Not needed")
        override suspend fun getActiveSlotsOnDate(epochMs: Long, parity: String?): List<TimetableSlotEntity> = error("Not needed")
        override suspend fun getAllSlotsOnce(): List<TimetableSlotEntity> = slotsList.toList()
        override suspend fun findMatchingSlot(subjectId: Long, dayOfWeek: Int, startTime: String, parity: String?): TimetableSlotEntity? =
            slotsList.firstOrNull { it.subjectId == subjectId && it.dayOfWeek == dayOfWeek && it.startTime == startTime }
    }

    private class FakeSubjectRepository : SubjectRepository {
        override fun getActiveSubjects(): Flow<List<SubjectEntity>> {
            return flowOf(listOf(SubjectEntity(id = 10, name = "Computer Science")))
        }
        override fun getAllSubjectsIncludingArchived(): Flow<List<SubjectEntity>> = error("Not needed")
        override fun getSubjectById(id: Long): Flow<SubjectEntity?> = error("Not needed")
        override suspend fun addSubject(name: String): AppResult<Long> = error("Not needed")
        override suspend fun renameSubject(id: Long, newName: String): AppResult<Unit> = error("Not needed")
        override suspend fun archiveSubject(id: Long): AppResult<Unit> = error("Not needed")
    }

    @Test
    fun init_loadsSlotsAndSubjectsSuccessfully() = runBlocking {
        val fakeDao = FakeTimetableSlotDao()
        val fakeSubjectRepo = FakeSubjectRepository()

        val viewModel = EditTimetableViewModel(
            timetableSlotDao = fakeDao,
            subjectRepository = fakeSubjectRepo
        )

        val state = viewModel.uiState.first { it is EditTimetableUiState.Success }
        assertTrue(state is EditTimetableUiState.Success)
        val success = state as EditTimetableUiState.Success
        assertEquals(1, success.slots.size)
        assertEquals(10L, success.slots[0].subjectId)
        assertEquals(1, success.subjects.size)
        assertEquals("Computer Science", success.subjects[0].name)
    }

    @Test
    fun selectDay_updatesSelectedDayOfWeek() = runBlocking {
        val fakeDao = FakeTimetableSlotDao()
        val fakeSubjectRepo = FakeSubjectRepository()

        val viewModel = EditTimetableViewModel(
            timetableSlotDao = fakeDao,
            subjectRepository = fakeSubjectRepo
        )

        viewModel.uiState.first { it is EditTimetableUiState.Success }
        viewModel.selectDay(3)
        delay(100)
        val state = viewModel.uiState.value as EditTimetableUiState.Success
        assertEquals(3, state.selectedDayOfWeek)
    }

    @Test
    fun deleteSlot_removesSlotFromState() = runBlocking {
        val fakeDao = FakeTimetableSlotDao()
        val fakeSubjectRepo = FakeSubjectRepository()

        val viewModel = EditTimetableViewModel(
            timetableSlotDao = fakeDao,
            subjectRepository = fakeSubjectRepo
        )

        viewModel.uiState.first { it is EditTimetableUiState.Success }
        viewModel.deleteSlot(1L)
        delay(100)
        val state = viewModel.uiState.value as EditTimetableUiState.Success
        assertTrue(state.slots.isEmpty())
    }
}
