package com.studentos.feature.attendance.presentation

import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.SettingEntity
import com.studentos.core.database.relation.SubjectAttendanceSummary
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import com.studentos.feature.attendance.presentation.state.AnalyticsUiState
import com.studentos.feature.attendance.presentation.viewmodel.AttendanceAnalyticsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceAnalyticsViewModelTest {

    private class FakeSettingsDao : SettingsDao {
        override suspend fun get(key: String): String? {
            return if (key == "attendance_threshold") "75" else null
        }
        override suspend fun getAll(): List<SettingEntity> = emptyList()
        override suspend fun set(setting: SettingEntity) {}
    }

    private class FakeClassEventRepository : ClassEventRepository {
        override fun getEventsForSubject(subjectId: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")
        override fun getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")

        override fun getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>> {
            return flowOf(
                listOf(
                    SubjectAttendanceSummary(
                        subjectId = 1,
                        subjectName = "Mathematics",
                        presentCount = 8,
                        absentCount = 2,
                        cancelledCount = 0,
                        holidayCount = 0,
                        extraPresentCount = 0,
                        totalHeldCount = 10,
                        percentage = 80.0
                    )
                )
            )
        }

        override suspend fun updateStatus(eventId: Long, status: String): AppResult<Unit> = error("Not needed")
        override suspend fun addExtraClass(
            subjectId: Long,
            scheduledAt: Long,
            endAt: Long,
            linkedSlotId: Long?
        ): AppResult<Long> = error("Not needed")
    }

    @Test
    fun init_loadsAnalyticsSuccessState() {
        runBlocking {
            val fakeRepo = FakeClassEventRepository()
            val fakeSettingsDao = FakeSettingsDao()

            val viewModel = AttendanceAnalyticsViewModel(
                classEventRepository = fakeRepo,
                settingsDao = fakeSettingsDao
            )

            val state = viewModel.uiState.first { it is AnalyticsUiState.Success }
            assertTrue(state is AnalyticsUiState.Success)
            val success = state as AnalyticsUiState.Success
            assertEquals(75, success.threshold)
            assertEquals(1, success.summaries.size)
            assertEquals("Mathematics", success.summaries[0].subjectName)
            assertEquals(80.0, success.overallPercentage, 0.01)
        }
    }
}
