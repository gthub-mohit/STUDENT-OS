package com.studentos.core.notifications.scheduler

import android.content.Context
import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.CpContestDao
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.CpContestEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.notifications.alarm.ExactAlarmScheduler
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class NotificationReschedulerTest {

    private val context: Context = mockk(relaxed = true)
    private val classEventDao: ClassEventDao = mockk(relaxed = true)
    private val subjectDao: SubjectDao = mockk(relaxed = true)
    private val assignmentDao: AssignmentDao = mockk(relaxed = true)
    private val cpContestDao: CpContestDao = mockk(relaxed = true)
    private val settingsDao: SettingsDao = mockk(relaxed = true)
    private val exactAlarmScheduler: ExactAlarmScheduler = mockk(relaxed = true)

    private lateinit var rescheduler: NotificationReschedulerImpl

    @Before
    fun setup() {
        rescheduler = NotificationReschedulerImpl(
            context = context,
            classEventDao = classEventDao,
            subjectDao = subjectDao,
            assignmentDao = assignmentDao,
            cpContestDao = cpContestDao,
            settingsDao = settingsDao,
            exactAlarmScheduler = exactAlarmScheduler
        )
    }

    @Test
    fun rescheduleAll_executesCleanlyAndSchedulesAlarms() = runTest {
        val now = System.currentTimeMillis()
        val futureEvent = ClassEventEntity(
            id = 20L,
            subjectId = 1L,
            scheduledAt = now + 1800_000L,
            endAt = now + 5400_000L,
            status = "UNMARKED",
            updatedAt = now
        )
        val futureAssignment = AssignmentEntity(
            id = 30L,
            subjectId = 1L,
            title = "Math HW",
            deadline = now + 7200_000L,
            priority = "HIGH",
            status = "PENDING",
            createdAt = now
        )
        val futureContest = CpContestEntity(
            id = 40L,
            profileId = 1L,
            contestName = "Weekly Contest 100",
            contestDate = now + 10_000_000L
        )

        coEvery { settingsDao.get(any()) } returns "true"
        coEvery { classEventDao.getEventsForWeek(any(), any()) } returns flowOf(listOf(futureEvent))
        coEvery { subjectDao.getSubjectById(1L) } returns flowOf(SubjectEntity(id = 1L, name = "Physics"))
        coEvery { assignmentDao.getAllAssignments() } returns flowOf(listOf(futureAssignment))
        coEvery { cpContestDao.getUpcomingContests(any(), any()) } returns listOf(futureContest)

        rescheduler.rescheduleAll()

        verify(atLeast = 1) {
            exactAlarmScheduler.scheduleExactAlarm(any(), any(), any(), any(), any(), any(), any())
        }
    }
}
