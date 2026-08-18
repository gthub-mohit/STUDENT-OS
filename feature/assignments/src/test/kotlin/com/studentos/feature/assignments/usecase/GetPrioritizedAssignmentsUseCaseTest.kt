package com.studentos.feature.assignments.usecase

import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.model.UrgencyCategory
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.usecase.GetPrioritizedAssignmentsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetPrioritizedAssignmentsUseCaseTest {

    private class FakeAssignmentRepository(
        private val list: List<AssignmentEntity>
    ) : AssignmentRepository {
        override fun getAssignmentById(id: Long): Flow<AssignmentEntity?> = error("Not needed")
        override fun getAllAssignments(): Flow<List<AssignmentEntity>> = flowOf(list)
        override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override suspend fun getUrgentAssignments(withinEpoch: Long): AppResult<List<AssignmentEntity>> = error("Not needed")
        override suspend fun createAssignment(assignment: AssignmentEntity): AppResult<Long> = error("Not needed")
        override suspend fun updateStatus(id: Long, newStatus: String): AppResult<Unit> = error("Not needed")
        override suspend fun updateDeadline(id: Long, deadline: Long): AppResult<Unit> = error("Not needed")
        override suspend fun updateReminderLead(id: Long, leadMs: Long?): AppResult<Unit> = error("Not needed")
        override suspend fun deleteAssignment(id: Long): AppResult<Unit> = error("Not needed")
        override suspend fun setAttachment(id: Long, uri: String?): AppResult<Unit> = error("Not needed")
        override suspend fun attachFile(id: Long, sourceUriString: String): AppResult<String> = error("Not needed")
    }

    @Test
    fun categorizeAndPrioritize_groupsAssignmentsCorrectlyByUrgency() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val todayLocalDate = LocalDate.now(zoneId)
        val nowEpoch = todayLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli() + 10 * 3600000L // 10:00 AM today
        val todayEpoch = todayLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli() + 14 * 3600000L // 2:00 PM today
        val tomorrowEpoch = todayLocalDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() + 14 * 3600000L
        val overdueEpoch = todayLocalDate.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() + 14 * 3600000L

        val items = listOf(
            AssignmentEntity(id = 1L, subjectId = 1L, title = "Overdue HW", deadline = overdueEpoch, priority = AssignmentEntity.PRIORITY_HIGH, status = AssignmentEntity.STATUS_PENDING, createdAt = nowEpoch),
            AssignmentEntity(id = 2L, subjectId = 1L, title = "Today HW", deadline = todayEpoch, priority = AssignmentEntity.PRIORITY_MEDIUM, status = AssignmentEntity.STATUS_PENDING, createdAt = nowEpoch),
            AssignmentEntity(id = 3L, subjectId = 1L, title = "Tomorrow HW", deadline = tomorrowEpoch, priority = AssignmentEntity.PRIORITY_LOW, status = AssignmentEntity.STATUS_PENDING, createdAt = nowEpoch)
        )

        val repo = FakeAssignmentRepository(items)
        val useCase = GetPrioritizedAssignmentsUseCase(repo)

        val groups = useCase.invoke(nowEpoch = nowEpoch, zoneId = zoneId).first()
        assertTrue(groups.isNotEmpty())

        val overdueGroup = groups.find { it.category == UrgencyCategory.OVERDUE }
        assertEquals(1, overdueGroup?.assignments?.size)
        assertEquals("Overdue HW", overdueGroup?.assignments?.get(0)?.title)

        val todayGroup = groups.find { it.category == UrgencyCategory.DUE_TODAY }
        assertEquals(1, todayGroup?.assignments?.size)
        assertEquals("Today HW", todayGroup?.assignments?.get(0)?.title)

        val tomorrowGroup = groups.find { it.category == UrgencyCategory.DUE_TOMORROW }
        assertEquals(1, tomorrowGroup?.assignments?.size)
        assertEquals("Tomorrow HW", tomorrowGroup?.assignments?.get(0)?.title)
    }

    @Test
    fun categorizeAndPrioritize_sortsChronologicallyByExactDeadlineWithinGroup() = runBlocking {
        val zoneId = ZoneId.systemDefault()
        val todayLocalDate = LocalDate.now(zoneId)
        val nowEpoch = todayLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val today2pm = todayLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli() + 14 * 3600000L // 2:00 PM
        val today10am = todayLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli() + 10 * 3600000L // 10:00 AM
        val laterDay = todayLocalDate.plusDays(10).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val items = listOf(
            AssignmentEntity(id = 1L, subjectId = 1L, title = "2 PM Task", deadline = today2pm, priority = AssignmentEntity.PRIORITY_HIGH, status = AssignmentEntity.STATUS_PENDING, createdAt = nowEpoch),
            AssignmentEntity(id = 2L, subjectId = 1L, title = "10 AM Task", deadline = today10am, priority = AssignmentEntity.PRIORITY_LOW, status = AssignmentEntity.STATUS_PENDING, createdAt = nowEpoch),
            AssignmentEntity(id = 3L, subjectId = 1L, title = "Later Task", deadline = laterDay, priority = AssignmentEntity.PRIORITY_MEDIUM, status = AssignmentEntity.STATUS_PENDING, createdAt = nowEpoch)
        )

        val repo = FakeAssignmentRepository(items)
        val useCase = GetPrioritizedAssignmentsUseCase(repo)

        val groups = useCase.invoke(nowEpoch = nowEpoch, zoneId = zoneId).first()

        val todayGroup = groups.find { it.category == UrgencyCategory.DUE_TODAY }
        assertEquals(2, todayGroup?.assignments?.size)
        // 10 AM task must appear before 2 PM task
        assertEquals("10 AM Task", todayGroup?.assignments?.get(0)?.title)
        assertEquals("2 PM Task", todayGroup?.assignments?.get(1)?.title)

        val laterGroup = groups.find { it.category == UrgencyCategory.LATER }
        assertEquals(1, laterGroup?.assignments?.size)
        assertEquals("Later Task", laterGroup?.assignments?.get(0)?.title)
    }
}
