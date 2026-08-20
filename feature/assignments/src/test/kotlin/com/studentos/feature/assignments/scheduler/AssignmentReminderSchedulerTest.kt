package com.studentos.feature.assignments.scheduler

import android.content.Context
import com.studentos.core.database.dao.SettingsDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.database.entity.SettingEntity
import com.studentos.feature.assignments.data.scheduler.AssignmentReminderSchedulerImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class AssignmentReminderSchedulerTest {

    private class FakeSettingsDao(
        private val settingsMap: MutableMap<String, String> = mutableMapOf()
    ) : SettingsDao {
        override suspend fun get(key: String): String? = settingsMap[key]
        override suspend fun getAll(): List<SettingEntity> = settingsMap.map { SettingEntity(it.key, it.value) }
        override fun observeAll(): kotlinx.coroutines.flow.Flow<List<SettingEntity>> =
            kotlinx.coroutines.flow.flowOf(settingsMap.map { SettingEntity(it.key, it.value) })
        override suspend fun set(setting: SettingEntity) {
            settingsMap[setting.key] = setting.value
        }
    }

    private class MinimalTestContext : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
    }

    private lateinit var settingsDao: FakeSettingsDao
    private lateinit var scheduler: AssignmentReminderSchedulerImpl

    private var enqueuedTag: String? = null
    private var enqueuedDelayMs: Long? = null
    private var enqueuedId: Long? = null
    private var cancelledTag: String? = null

    @Before
    fun setUp() {
        settingsDao = FakeSettingsDao()
        scheduler = AssignmentReminderSchedulerImpl(MinimalTestContext(), settingsDao).apply {
            enqueueWorkDelegate = { tag, delayMs, id ->
                enqueuedTag = tag
                enqueuedDelayMs = delayMs
                enqueuedId = id
            }
            cancelWorkDelegate = { tag ->
                cancelledTag = tag
            }
        }
        enqueuedTag = null
        enqueuedDelayMs = null
        enqueuedId = null
        cancelledTag = null
    }

    @Test
    fun scheduleReminder_validFutureAssignment_schedulesCorrectly() = runBlocking {
        val now = System.currentTimeMillis()
        val assignment = AssignmentEntity(
            id = 10L,
            subjectId = 1L,
            title = "Algorithmic Complexity",
            deadline = now + 7200000L, // 2 hours in future
            reminderLeadMs = 3600000L, // 1 hour lead
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = now
        )

        scheduler.scheduleReminder(assignment)

        assertEquals("assignment_10", enqueuedTag)
        assertEquals(10L, enqueuedId)
        assertNotNull(enqueuedDelayMs)
        // Expected delay approx 1 hour (3600000ms)
        assertTrue((enqueuedDelayMs ?: 0L) in 3500000L..3700000L)
    }

    @Test
    fun scheduleReminder_rescheduleActiveAssignment_replacesExistingReminderTag() = runBlocking {
        val now = System.currentTimeMillis()
        val assignment = AssignmentEntity(
            id = 10L,
            subjectId = 1L,
            title = "Algorithmic Complexity",
            deadline = now + 7200000L,
            reminderLeadMs = 1800000L, // 30 min lead
            status = AssignmentEntity.STATUS_IN_PROGRESS,
            createdAt = now
        )

        scheduler.scheduleReminder(assignment)

        assertEquals("assignment_10", enqueuedTag)
        assertTrue((enqueuedDelayMs ?: 0L) in 5300000L..5500000L)
    }

    @Test
    fun scheduleReminder_submittedStatus_cancelsReminder() = runBlocking {
        val now = System.currentTimeMillis()
        val assignment = AssignmentEntity(
            id = 15L,
            subjectId = 1L,
            title = "Submitted HW",
            deadline = now + 7200000L,
            status = AssignmentEntity.STATUS_SUBMITTED,
            createdAt = now
        )

        scheduler.scheduleReminder(assignment)

        assertNull(enqueuedTag)
        assertEquals("assignment_15", cancelledTag)
    }

    @Test
    fun scheduleReminder_completedStatus_cancelsReminder() = runBlocking {
        val now = System.currentTimeMillis()
        val assignment = AssignmentEntity(
            id = 20L,
            subjectId = 1L,
            title = "Completed HW",
            deadline = now + 7200000L,
            status = AssignmentEntity.STATUS_COMPLETED,
            createdAt = now
        )

        scheduler.scheduleReminder(assignment)

        assertNull(enqueuedTag)
        assertEquals("assignment_20", cancelledTag)
    }

    @Test
    fun scheduleReminder_nullLeadMs_usesGlobalSettingsFallback() = runBlocking {
        val now = System.currentTimeMillis()
        settingsDao.set(SettingEntity("default_assignment_reminder_lead_ms", "1800000")) // 30 min global fallback

        val assignment = AssignmentEntity(
            id = 25L,
            subjectId = 1L,
            title = "Fallback Test",
            deadline = now + 7200000L, // 2 hours
            reminderLeadMs = null,
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = now
        )

        scheduler.scheduleReminder(assignment)

        assertEquals("assignment_25", enqueuedTag)
        // Expected delay = 2h - 30m = 1.5h = 5400000ms
        assertTrue((enqueuedDelayMs ?: 0L) in 5300000L..5500000L)
    }

    @Test
    fun scheduleReminder_perAssignmentLeadMs_overridesGlobalSettings() = runBlocking {
        val now = System.currentTimeMillis()
        settingsDao.set(SettingEntity("default_assignment_reminder_lead_ms", "3600000")) // 1h global

        val assignment = AssignmentEntity(
            id = 30L,
            subjectId = 1L,
            title = "Override Test",
            deadline = now + 7200000L, // 2 hours
            reminderLeadMs = 900000L, // 15 min per-assignment override
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = now
        )

        scheduler.scheduleReminder(assignment)

        assertEquals("assignment_30", enqueuedTag)
        // Expected delay = 2h - 15m = 1h 45m = 6300000ms
        assertTrue((enqueuedDelayMs ?: 0L) in 6200000L..6400000L)
    }

    @Test
    fun scheduleReminder_pastTriggerTime_doesNotSchedule() = runBlocking {
        val now = System.currentTimeMillis()
        val assignment = AssignmentEntity(
            id = 35L,
            subjectId = 1L,
            title = "Past Deadline HW",
            deadline = now + 1000L, // Only 1s in future
            reminderLeadMs = 3600000L, // 1h lead -> trigger was in past!
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = now
        )

        scheduler.scheduleReminder(assignment)

        assertNull(enqueuedTag)
        assertEquals("assignment_35", cancelledTag)
    }
}
