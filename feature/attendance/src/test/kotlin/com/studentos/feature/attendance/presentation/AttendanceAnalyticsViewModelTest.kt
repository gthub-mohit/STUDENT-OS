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

    @Test
    fun multipleSubjects_independentAttendanceAndCalculations() {
        runBlocking {
            val multiSubjectRepo = object : ClassEventRepository {
                override fun getEventsForSubject(subjectId: Long): Flow<List<ClassEventEntity>> = error("Not needed")
                override fun getEventsForDay(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")
                override fun getEventsForWeek(startEpoch: Long, endEpoch: Long): Flow<List<ClassEventEntity>> = error("Not needed")

                override fun getAllAttendanceSummaries(): Flow<List<SubjectAttendanceSummary>> {
                    return flowOf(
                        listOf(
                            SubjectAttendanceSummary(
                                subjectId = 101,
                                subjectName = "ME201",
                                presentCount = 14,
                                absentCount = 3,
                                cancelledCount = 2,
                                holidayCount = 0,
                                extraPresentCount = 0,
                                totalHeldCount = 17,
                                percentage = 82.35
                            ),
                            SubjectAttendanceSummary(
                                subjectId = 102,
                                subjectName = "CS203",
                                presentCount = 11,
                                absentCount = 5,
                                cancelledCount = 1,
                                holidayCount = 0,
                                extraPresentCount = 0,
                                totalHeldCount = 16,
                                percentage = 68.75
                            ),
                            SubjectAttendanceSummary(
                                subjectId = 103,
                                subjectName = "VAC202",
                                presentCount = 0,
                                absentCount = 0,
                                cancelledCount = 0,
                                holidayCount = 0,
                                extraPresentCount = 0,
                                totalHeldCount = 0,
                                percentage = 0.0
                            )
                        )
                    )
                }

                override suspend fun updateStatus(eventId: Long, status: String): AppResult<Unit> = error("Not needed")
                override suspend fun addExtraClass(subjectId: Long, scheduledAt: Long, endAt: Long, linkedSlotId: Long?): AppResult<Long> = error("Not needed")
            }

            val viewModel = AttendanceAnalyticsViewModel(
                classEventRepository = multiSubjectRepo,
                settingsDao = FakeSettingsDao()
            )

            val state = viewModel.uiState.first { it is AnalyticsUiState.Success } as AnalyticsUiState.Success
            assertEquals(3, state.summaries.size)

            val me201 = state.summaries[0]
            assertEquals("ME201", me201.subjectName)
            assertEquals(14, me201.presentCount)
            assertEquals(17, me201.totalHeldCount)
            assertEquals(82.35, me201.percentage, 0.01)

            val cs203 = state.summaries[1]
            assertEquals("CS203", cs203.subjectName)
            assertEquals(11, cs203.presentCount)
            assertEquals(16, cs203.totalHeldCount)
            assertEquals(68.75, cs203.percentage, 0.01)

            val vac202 = state.summaries[2]
            assertEquals("VAC202", vac202.subjectName)
            assertEquals(0, vac202.totalHeldCount)
        }
    }

    @Test
    fun getAttendanceStatusMessage_deterministicContextualMessages() {
        val zeroData = com.studentos.feature.attendance.presentation.screen.getAttendanceStatusMessage(0.0, 0)
        assertEquals("No attendance recorded yet — mark your first class", zeroData)

        val above90 = com.studentos.feature.attendance.presentation.screen.getAttendanceStatusMessage(92.5, 20)
        assertEquals("You're comfortably above your target.", above90)

        val above85 = com.studentos.feature.attendance.presentation.screen.getAttendanceStatusMessage(87.0, 20)
        assertEquals("Looking solid. You have a healthy attendance buffer.", above85)

        val above75 = com.studentos.feature.attendance.presentation.screen.getAttendanceStatusMessage(78.5, 20)
        assertEquals("You're on track. Keep a little buffer for unexpected absences.", above75)

        val above70 = com.studentos.feature.attendance.presentation.screen.getAttendanceStatusMessage(72.0, 20)
        assertEquals("You're getting close to the danger zone. Be a little careful with your next few classes.", above70)

        val below70 = com.studentos.feature.attendance.presentation.screen.getAttendanceStatusMessage(64.0, 20)
        assertEquals("Attendance needs attention. Prioritize upcoming classes to recover.", below70)
    }
}
