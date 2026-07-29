package com.studentos.feature.attendance.presentation

import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.SettingEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import com.studentos.feature.attendance.domain.repository.SubjectRepository
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
        override suspend fun set(setting: SettingEntity) {}
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
    }

    private class FakeClassEventRepository : ClassEventRepository {
        var updatedStatus: String? = null

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
        ): AppResult<Long> = error("Not needed")
    }

    @Test
    fun init_loadsSuccessStateWithEventsAndPercentage() {
        runBlocking {
            val fakeRepo = FakeClassEventRepository()
            val fakeSubjectRepo = FakeSubjectRepository()
            val fakeSettingsDao = FakeSettingsDao()
            val updateUseCase = UpdateClassEventStatusUseCase(fakeRepo)
            val addUseCase = AddExtraClassUseCase(fakeRepo)

            val viewModel = WeeklyViewModel(
                classEventRepository = fakeRepo,
                subjectRepository = fakeSubjectRepo,
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
        }
    }

    @Test
    fun selectDay_updatesSelectedDayOfWeek() {
        runBlocking {
            val fakeRepo = FakeClassEventRepository()
            val fakeSubjectRepo = FakeSubjectRepository()
            val fakeSettingsDao = FakeSettingsDao()
            val updateUseCase = UpdateClassEventStatusUseCase(fakeRepo)
            val addUseCase = AddExtraClassUseCase(fakeRepo)

            val viewModel = WeeklyViewModel(
                classEventRepository = fakeRepo,
                subjectRepository = fakeSubjectRepo,
                settingsDao = fakeSettingsDao,
                updateClassEventStatusUseCase = updateUseCase,
                addExtraClassUseCase = addUseCase
            )

            val initialState = viewModel.uiState.first { it is WeeklyUiState.Success }
            viewModel.selectDay(3)
            val updatedState = viewModel.uiState.first { (it as? WeeklyUiState.Success)?.selectedDayOfWeek == 3 } as WeeklyUiState.Success
            assertEquals(3, updatedState.selectedDayOfWeek)
        }
    }
}
