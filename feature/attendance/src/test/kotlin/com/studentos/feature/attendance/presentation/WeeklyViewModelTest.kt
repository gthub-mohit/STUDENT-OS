package com.studentos.feature.attendance.presentation

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.SettingEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import com.studentos.feature.attendance.domain.repository.TimetableRepository
import com.studentos.feature.attendance.domain.usecase.AddExtraClassUseCase
import com.studentos.feature.attendance.domain.usecase.UpdateClassEventStatusUseCase
import com.studentos.feature.attendance.presentation.state.WeeklyUiState
import com.studentos.feature.attendance.presentation.viewmodel.WeeklyViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyViewModelTest {

    private class FakeSettingsDao : SettingsDao {
        override suspend fun get(key: String): String? {
            return if (key == "attendance_threshold") "75" else null
        }
        override suspend fun getAll(): List<SettingEntity> = emptyList()
        override fun observeAll(): Flow<List<SettingEntity>> = flowOf(emptyList())
        override suspend fun set(setting: SettingEntity) {}
    }

    private class FakeTimetableRepository : TimetableRepository {
        override fun getAllSlots(): Flow<List<TimetableSlotEntity>> = flowOf(emptyList())
        override suspend fun importTimetable(
            slots: List<ParsedTimetableSlot>,
            replaceExisting: Boolean,
            horizonDays: Int
        ): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun addSlot(slot: TimetableSlotEntity, horizonDays: Int): AppResult<Long> = AppResult.Success(1L)
        override suspend fun updateSlot(slot: TimetableSlotEntity, horizonDays: Int): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun deleteSlot(slotId: Long): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeSubjectRepository : SubjectRepository {
        override fun getActiveSubjects(): Flow<List<SubjectEntity>> {
            return flowOf(listOf(SubjectEntity(id = 1, name = "Maths")))
        }
        override fun getAllSubjectsIncludingArchived(): Flow<List<SubjectEntity>> = error("Not needed")
        override fun getSubjectById(id: Long): Flow<SubjectEntity?> = error("Not needed")
        override suspend fun addSubject(name: String): AppResult<Long> = error("Not needed")
        override suspend fun renameSubject(id: Long, newName: String): AppResult<Unit> = error("Not needed")
        override suspend fun archiveSubject(id: Long): AppResult<Unit> = error("Not needed")
        override suspend fun cleanupInvalidOcrSubjects(targetNames: List<String>): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeClassEventRepository : ClassEventRepository {
        var updatedStatus: String? = null
        var addedExtraSubjectId: Long? = null

        override fun getEventsForSubject(subjectId: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> {
            return flowOf(
                listOf(
                    ClassEventEntity(
                        id = 10,
                        subjectId = 1,
                        scheduledAt = System.currentTimeMillis(),
                        endAt = System.currentTimeMillis() + 3600000,
                        status = ClassEventEntity.STATUS_PRESENT,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            )
        }

        override fun getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>> = error("Not needed")

        override suspend fun updateStatus(eventId: Long, status: String): AppResult<Unit> {
            updatedStatus = status
            return AppResult.Success(Unit)
        }

        override suspend fun addExtraClass(
            subjectId: Long,
            scheduledAt: Long,
            endAt: Long,
            linkedSlotId: Long?
        ): AppResult<Long> {
            addedExtraSubjectId = subjectId
            return AppResult.Success(99L)
        }
    }

    @Test
    fun init_loadsSuccessStateWithEventsAndPercentage() {
        runBlocking {
            val fakeRepo = FakeClassEventRepository()
            val fakeSubjectRepo = FakeSubjectRepository()
            val fakeTimetableRepo = FakeTimetableRepository()
            val fakeSettingsDao = FakeSettingsDao()
            val updateUseCase = UpdateClassEventStatusUseCase(fakeRepo)
            val addUseCase = AddExtraClassUseCase(fakeRepo)

            val viewModel = WeeklyViewModel(
                classEventRepository = fakeRepo,
                subjectRepository = fakeSubjectRepo,
                timetableRepository = fakeTimetableRepo,
                settingsDao = fakeSettingsDao,
                updateClassEventStatusUseCase = updateUseCase,
                addExtraClassUseCase = addUseCase
            )

            val state = viewModel.uiState.first { it is WeeklyUiState.Success }
            assertTrue(state is WeeklyUiState.Success)
            val success = state as WeeklyUiState.Success
            assertEquals(75, success.threshold)
            assertEquals(1, success.subjects.size)
            assertEquals("Maths", success.subjects[0].name)
            assertEquals(0, success.weekOffset)
            assertEquals("This Week", success.weekLabel)
        }
    }

    @Test
    fun selectDay_updatesSelectedDayOfWeek() {
        runBlocking {
            val fakeRepo = FakeClassEventRepository()
            val fakeSubjectRepo = FakeSubjectRepository()
            val fakeTimetableRepo = FakeTimetableRepository()
            val fakeSettingsDao = FakeSettingsDao()
            val updateUseCase = UpdateClassEventStatusUseCase(fakeRepo)
            val addUseCase = AddExtraClassUseCase(fakeRepo)

            val viewModel = WeeklyViewModel(
                classEventRepository = fakeRepo,
                subjectRepository = fakeSubjectRepo,
                timetableRepository = fakeTimetableRepo,
                settingsDao = fakeSettingsDao,
                updateClassEventStatusUseCase = updateUseCase,
                addExtraClassUseCase = addUseCase
            )

            viewModel.selectDay(3)
            val updatedState = viewModel.uiState.first { (it as? WeeklyUiState.Success)?.selectedDayOfWeek == 3 } as WeeklyUiState.Success
            assertEquals(3, updatedState.selectedDayOfWeek)
        }
    }

    @Test
    fun weekNavigation_previousAndNextWeek() {
        runBlocking {
            val fakeRepo = FakeClassEventRepository()
            val fakeSubjectRepo = FakeSubjectRepository()
            val fakeTimetableRepo = FakeTimetableRepository()
            val fakeSettingsDao = FakeSettingsDao()
            val updateUseCase = UpdateClassEventStatusUseCase(fakeRepo)
            val addUseCase = AddExtraClassUseCase(fakeRepo)

            val viewModel = WeeklyViewModel(
                classEventRepository = fakeRepo,
                subjectRepository = fakeSubjectRepo,
                timetableRepository = fakeTimetableRepo,
                settingsDao = fakeSettingsDao,
                updateClassEventStatusUseCase = updateUseCase,
                addExtraClassUseCase = addUseCase
            )

            viewModel.previousWeek()
            val prevWeekState = viewModel.uiState.first { (it as? WeeklyUiState.Success)?.weekOffset == -1 } as WeeklyUiState.Success
            assertEquals(-1, prevWeekState.weekOffset)

            viewModel.nextWeek()
            val currentWeekState = viewModel.uiState.first { (it as? WeeklyUiState.Success)?.weekOffset == 0 } as WeeklyUiState.Success
            assertEquals(0, currentWeekState.weekOffset)
        }
    }

    @Test
    fun init_withMarkedEvents_calculatesTotalHeldCount() = runBlocking {
        val fakeRepo = FakeClassEventRepository()
        val fakeSubjectRepo = FakeSubjectRepository()
        val fakeTimetableRepo = FakeTimetableRepository()
        val fakeSettingsDao = FakeSettingsDao()
        val updateUseCase = UpdateClassEventStatusUseCase(fakeRepo)
        val addUseCase = AddExtraClassUseCase(fakeRepo)

        val viewModel = WeeklyViewModel(
            classEventRepository = fakeRepo,
            subjectRepository = fakeSubjectRepo,
            timetableRepository = fakeTimetableRepo,
            settingsDao = fakeSettingsDao,
            updateClassEventStatusUseCase = updateUseCase,
            addExtraClassUseCase = addUseCase
        )

        val state = viewModel.uiState.first { it is WeeklyUiState.Success } as WeeklyUiState.Success
        assertEquals(1, state.totalHeldCount)
        assertEquals(100.0, state.overallAttendancePercentage, 0.01)
        assertEquals(false, state.isBelowThreshold)
    }
}
