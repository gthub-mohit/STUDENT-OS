package com.studentos.feature.attendance.presentation

import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import com.studentos.feature.attendance.domain.repository.TimetableRepository
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

    private class FakeTimetableRepository : TimetableRepository {
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

        override fun getAllSlots(): Flow<List<TimetableSlotEntity>> = slotsFlow

        override suspend fun importTimetable(
            slots: List<ParsedTimetableSlot>,
            replaceExisting: Boolean,
            horizonDays: Int
        ): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun addSlot(
            slot: TimetableSlotEntity,
            horizonDays: Int
        ): AppResult<Long> {
            val newSlot = slot.copy(id = (slotsList.size + 1).toLong())
            slotsList.add(newSlot)
            slotsFlow.value = slotsList.toList()
            return AppResult.Success(newSlot.id)
        }

        override suspend fun updateSlot(
            slot: TimetableSlotEntity,
            horizonDays: Int
        ): AppResult<Unit> {
            val index = slotsList.indexOfFirst { it.id == slot.id }
            if (index != -1) {
                slotsList[index] = slot
                slotsFlow.value = slotsList.toList()
            }
            return AppResult.Success(Unit)
        }

        override suspend fun deleteSlot(
            slotId: Long
        ): AppResult<Unit> {
            slotsList.removeAll { it.id == slotId }
            slotsFlow.value = slotsList.toList()
            return AppResult.Success(Unit)
        }
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
        override suspend fun cleanupInvalidOcrSubjects(targetNames: List<String>): AppResult<Unit> = AppResult.Success(Unit)
    }

    @Test
    fun init_loadsSlotsAndSubjectsSuccessfully() = runBlocking {
        val fakeRepo = FakeTimetableRepository()
        val fakeSubjectRepo = FakeSubjectRepository()

        val viewModel = EditTimetableViewModel(
            timetableRepository = fakeRepo,
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
        val fakeRepo = FakeTimetableRepository()
        val fakeSubjectRepo = FakeSubjectRepository()

        val viewModel = EditTimetableViewModel(
            timetableRepository = fakeRepo,
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
        val fakeRepo = FakeTimetableRepository()
        val fakeSubjectRepo = FakeSubjectRepository()

        val viewModel = EditTimetableViewModel(
            timetableRepository = fakeRepo,
            subjectRepository = fakeSubjectRepo
        )

        viewModel.uiState.first { it is EditTimetableUiState.Success }
        viewModel.deleteSlot(1L)
        delay(100)
        val state = viewModel.uiState.value as EditTimetableUiState.Success
        assertEquals(0, state.slots.size)
    }

    @Test
    fun addSlot_addsSlotToState() = runBlocking {
        val fakeRepo = FakeTimetableRepository()
        val fakeSubjectRepo = FakeSubjectRepository()

        val viewModel = EditTimetableViewModel(
            timetableRepository = fakeRepo,
            subjectRepository = fakeSubjectRepo
        )

        viewModel.uiState.first { it is EditTimetableUiState.Success }
        viewModel.addSlot(
            subjectId = 10L,
            dayOfWeek = 2,
            startTime = "11:00",
            endTime = "12:00",
            location = "C104",
            weekParity = null
        )
        delay(100)
        val state = viewModel.uiState.value as EditTimetableUiState.Success
        assertEquals(2, state.slots.size)
    }

    @Test
    fun updateSlot_updatesSlotInState() = runBlocking {
        val fakeRepo = FakeTimetableRepository()
        val fakeSubjectRepo = FakeSubjectRepository()

        val viewModel = EditTimetableViewModel(
            timetableRepository = fakeRepo,
            subjectRepository = fakeSubjectRepo
        )

        viewModel.uiState.first { it is EditTimetableUiState.Success }
        val updatedSlot = fakeRepo.slotsList[0].copy(startTime = "14:00", endTime = "15:00")
        viewModel.updateSlot(updatedSlot)
        delay(100)
        val state = viewModel.uiState.value as EditTimetableUiState.Success
        assertEquals("14:00", state.slots[0].startTime)
    }
}
