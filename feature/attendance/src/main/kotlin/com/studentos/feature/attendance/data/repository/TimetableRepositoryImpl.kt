package com.studentos.feature.attendance.data.repository

import androidx.room.withTransaction
import com.studentos.core.database.AppDatabase
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.database.entity.ClassEventEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.database.entity.TimetableSlotEntity
import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.core.notifications.scheduler.NotificationRescheduler
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import com.studentos.feature.attendance.domain.repository.TimetableRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

/**
 * TimetableRepositoryImpl — Data repository implementing transactional timetable import and class event generation.
 */
class TimetableRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val subjectDao: SubjectDao,
    private val timetableSlotDao: TimetableSlotDao,
    private val classEventDao: ClassEventDao,
    private val notificationRescheduler: NotificationRescheduler? = null
) : TimetableRepository {

    override fun getAllSlots(): Flow<List<TimetableSlotEntity>> {
        return timetableSlotDao.getAllSlots()
    }

    override suspend fun importTimetable(
        slots: List<ParsedTimetableSlot>,
        replaceExisting: Boolean,
        horizonDays: Int
    ): AppResult<Unit> {
        return try {
            database.withTransaction {
                if (!replaceExisting) {
                    val existingSubjectCount = subjectDao.getSubjectCount()
                    if (existingSubjectCount > 0) {
                        return@withTransaction AppResult.Failure(
                            AppError.ValidationError("Existing timetable events present. User confirmation required.")
                        )
                    }
                }

                val now = System.currentTimeMillis()

                if (replaceExisting) {
                    // ── Dependency-Aware Replacement ──────────────────────────
                    // FK chain: subjects ←[RESTRICT]─ timetable_slots ←[RESTRICT]─ class_events
                    // Must remove/detach children BEFORE deleting parents.

                    val oldSlots = timetableSlotDao.getAllSlotsOnce()
                    val oldSlotIds = oldSlots.map { it.id }

                    if (oldSlotIds.isNotEmpty()) {
                        // Step 1: Delete only auto-generated UNMARKED events (future placeholders)
                        classEventDao.deleteUnmarkedBySlotIds(oldSlotIds)

                        // Step 2: Detach marked events (PRESENT/ABSENT/CANCELLED/HOLIDAY/EXTRA_CLASS)
                        // by nullifying their FK references. This preserves attendance history
                        // while allowing the old timetable slots to be deleted safely.
                        classEventDao.nullifySlotReferences(oldSlotIds, now)
                    }

                    // Step 3: Now safe to delete old timetable slots — no FKs reference them
                    timetableSlotDao.deleteAll()
                }

                // ── 1. Deduplicate template definitions ───────────────────
                val uniqueSlots = slots.distinctBy {
                    Triple(it.subjectName.trim().uppercase(), it.dayOfWeek, it.startTime.trim())
                }

                // ── 2. Insert/Resolve Recurring Timetable Template Slots ──
                val slotMapping = mutableListOf<Triple<ParsedTimetableSlot, Long, Long>>() // (slot, slotId, subjectId)

                for (slot in uniqueSlots) {
                    val trimmedSubjName = slot.subjectName.trim()
                    val existingSubject = subjectDao.getByName(trimmedSubjName)
                    val subjectId = existingSubject?.id ?: subjectDao.insert(
                        SubjectEntity(name = trimmedSubjName)
                    )

                    val existingSlot = if (!replaceExisting) {
                        timetableSlotDao.findMatchingSlot(subjectId, slot.dayOfWeek, slot.startTime, null)
                    } else null

                    val slotId = existingSlot?.id ?: timetableSlotDao.insert(
                        TimetableSlotEntity(
                            subjectId = subjectId,
                            dayOfWeek = slot.dayOfWeek,
                            startTime = slot.startTime,
                            endTime = slot.endTime,
                            location = slot.location,
                            validFrom = now
                        )
                    )

                    slotMapping.add(Triple(slot, slotId, subjectId))
                }

                // ── 3. Populate Recurring Monday–Sunday Pattern as Date-Specific Events ──
                val nowCal = Calendar.getInstance()
                val currentDow = nowCal.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = when (currentDow) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                val startCal = (nowCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -daysFromMonday)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                for (dayOffset in 0 until horizonDays) {
                    val currentDayCal = (startCal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                    }

                    val currentDayOfWeek = when (currentDayCal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        Calendar.SUNDAY -> 7
                        else -> 1
                    }

                    val daySlots = slotMapping.filter { it.first.dayOfWeek == currentDayOfWeek }

                    for ((slot, slotId, subjectId) in daySlots) {
                        val (startH, startM) = parseHourMinute(slot.startTime)
                        val (endH, endM) = parseHourMinute(slot.endTime)

                        val eventCal = (currentDayCal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, startH)
                            set(Calendar.MINUTE, startM)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val scheduledAt = eventCal.timeInMillis

                        val endEventCal = (currentDayCal.clone() as Calendar).apply {
                            set(Calendar.HOUR_OF_DAY, endH)
                            set(Calendar.MINUTE, endM)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endAt = if (endEventCal.timeInMillis > scheduledAt) {
                            endEventCal.timeInMillis
                        } else {
                            scheduledAt + parseDurationMs(slot.startTime, slot.endTime)
                        }

                        // Avoid duplicating class events or destroying existing marked history
                        val existingEvent = classEventDao.getEventBySubjectAndSchedule(subjectId, scheduledAt)
                        if (existingEvent == null) {
                            classEventDao.insert(
                                ClassEventEntity(
                                    timetableSlotId = slotId,
                                    subjectId = subjectId,
                                    scheduledAt = scheduledAt,
                                    endAt = endAt,
                                    status = ClassEventEntity.STATUS_UNMARKED,
                                    updatedAt = now
                                )
                            )
                        }
                    }
                }

                AppResult.Success(Unit)
            }.also { result ->
                if (result is AppResult.Success) {
                    try { notificationRescheduler?.rescheduleClassReminders() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Database import failure"))
        }
    }

    override suspend fun addSlot(
        slot: TimetableSlotEntity,
        horizonDays: Int
    ): AppResult<Long> {
        return try {
            database.withTransaction {
                val now = System.currentTimeMillis()
                val slotId = timetableSlotDao.insert(slot)
                val insertedSlot = slot.copy(id = slotId)
                populateEventsForSlot(insertedSlot, horizonDays, now)
                AppResult.Success(slotId)
            }.also { result ->
                if (result is AppResult.Success) {
                    try { notificationRescheduler?.rescheduleClassReminders() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to add timetable slot"))
        }
    }

    override suspend fun updateSlot(
        slot: TimetableSlotEntity,
        horizonDays: Int
    ): AppResult<Unit> {
        return try {
            database.withTransaction {
                val now = System.currentTimeMillis()
                classEventDao.deleteUnmarkedBySlotIds(listOf(slot.id))
                classEventDao.nullifySlotReferences(listOf(slot.id), now)
                timetableSlotDao.update(slot)
                populateEventsForSlot(slot, horizonDays, now)
                AppResult.Success(Unit)
            }.also { result ->
                if (result is AppResult.Success) {
                    try { notificationRescheduler?.rescheduleClassReminders() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to update timetable slot"))
        }
    }

    override suspend fun deleteSlot(
        slotId: Long
    ): AppResult<Unit> {
        return try {
            database.withTransaction {
                val now = System.currentTimeMillis()
                classEventDao.deleteUnmarkedBySlotIds(listOf(slotId))
                classEventDao.nullifySlotReferences(listOf(slotId), now)
                timetableSlotDao.deleteById(slotId)
                AppResult.Success(Unit)
            }.also { result ->
                if (result is AppResult.Success) {
                    try { notificationRescheduler?.rescheduleClassReminders() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to delete timetable slot"))
        }
    }

    private suspend fun populateEventsForSlot(
        slot: TimetableSlotEntity,
        horizonDays: Int,
        now: Long
    ) {
        val (startH, startM) = parseHourMinute(slot.startTime)
        val (endH, endM) = parseHourMinute(slot.endTime)

        val nowCal = Calendar.getInstance()
        val currentDow = nowCal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (currentDow) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        val startCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -daysFromMonday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (dayOffset in 0 until horizonDays) {
            val currentDayCal = (startCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            val currentDayOfWeek = when (currentDayCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            if (currentDayOfWeek == slot.dayOfWeek) {
                val eventCal = (currentDayCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, startH)
                    set(Calendar.MINUTE, startM)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val scheduledAt = eventCal.timeInMillis

                val endEventCal = (currentDayCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, endH)
                    set(Calendar.MINUTE, endM)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endAt = if (endEventCal.timeInMillis > scheduledAt) {
                    endEventCal.timeInMillis
                } else {
                    scheduledAt + parseDurationMs(slot.startTime, slot.endTime)
                }

                val existingEvent = classEventDao.getEventBySubjectAndSchedule(slot.subjectId, scheduledAt)
                if (existingEvent == null) {
                    classEventDao.insert(
                        ClassEventEntity(
                            timetableSlotId = slot.id,
                            subjectId = slot.subjectId,
                            scheduledAt = scheduledAt,
                            endAt = endAt,
                            status = ClassEventEntity.STATUS_UNMARKED,
                            updatedAt = now
                        )
                    )
                }
            }
        }
    }

    private fun parseHourMinute(time: String): Pair<Int, Int> {
        return try {
            val parts = time.split(":").map { it.trim().toInt() }
            val hour = if (parts.isNotEmpty()) parts[0] else 8
            val minute = if (parts.size > 1) parts[1] else 0
            Pair(hour, minute)
        } catch (_: Exception) {
            Pair(8, 0)
        }
    }

    private fun parseDurationMs(startTime: String, endTime: String): Long {
        return try {
            val (startH, startM) = parseHourMinute(startTime)
            val (endH, endM) = parseHourMinute(endTime)
            val startMinutes = startH * 60 + startM
            val endMinutes = endH * 60 + endM
            val diffMinutes = if (endMinutes > startMinutes) endMinutes - startMinutes else 60
            diffMinutes * 60 * 1000L
        } catch (e: Exception) {
            3600000L
        }
    }
}
