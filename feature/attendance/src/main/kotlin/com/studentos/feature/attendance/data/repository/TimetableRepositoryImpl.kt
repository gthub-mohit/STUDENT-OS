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
import com.studentos.feature.attendance.domain.model.ParsedTimetableSlot
import com.studentos.feature.attendance.domain.repository.TimetableRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * TimetableRepositoryImpl — Data repository implementing transactional timetable import and class event generation.
 */
class TimetableRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val subjectDao: SubjectDao,
    private val timetableSlotDao: TimetableSlotDao,
    private val classEventDao: ClassEventDao
) : TimetableRepository {

    override suspend fun importTimetable(
        slots: List<ParsedTimetableSlot>,
        replaceExisting: Boolean,
        horizonDays: Int
    ): AppResult<Unit> {
        return try {
            database.withTransaction {
                // If replaceExisting == false, check if subjects exist
                if (!replaceExisting) {
                    val existingSubjectCount = subjectDao.getSubjectCount()
                    if (existingSubjectCount > 0) {
                        return@withTransaction AppResult.Failure(
                            AppError.ValidationError("Existing timetable events present. User confirmation required.")
                        )
                    }
                }

                // Delete existing timetable slots if replacing
                if (replaceExisting) {
                    timetableSlotDao.deleteAll()
                }

                val now = System.currentTimeMillis()

                // Process each slot
                for (slot in slots) {
                    // 1. Get or create subject
                    val existingSubject = subjectDao.getByName(slot.subjectName)
                    val subjectId = existingSubject?.id ?: subjectDao.insert(
                        SubjectEntity(name = slot.subjectName)
                    )

                    // 2. Insert TimetableSlot
                    val slotId = timetableSlotDao.insert(
                        TimetableSlotEntity(
                            subjectId = subjectId,
                            dayOfWeek = slot.dayOfWeek,
                            startTime = slot.startTime,
                            endTime = slot.endTime,
                            location = slot.location,
                            validFrom = now
                        )
                    )

                    // 3. Generate ClassEvents for next N days matching slot.dayOfWeek
                    val cal = Calendar.getInstance()
                    for (dayOffset in 0 until horizonDays) {
                        val currentDayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
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
                            val scheduledAt = cal.timeInMillis
                            val endAt = scheduledAt + parseDurationMs(slot.startTime, slot.endTime)

                            classEventDao.insert(
                                ClassEventEntity(
                                    timetableSlotId = slotId,
                                    subjectId = subjectId,
                                    scheduledAt = scheduledAt,
                                    endAt = endAt,
                                    status = ClassEventEntity.STATUS_PRESENT,
                                    updatedAt = now
                                )
                            )
                        }

                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                AppResult.Success(Unit)
            }
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Database import failure"))
        }
    }

    private fun parseDurationMs(startTime: String, endTime: String): Long {
        return try {
            val startParts = startTime.split(":").map { it.toInt() }
            val endParts = endTime.split(":").map { it.toInt() }
            val startMinutes = startParts[0] * 60 + startParts[1]
            val endMinutes = endParts[0] * 60 + endParts[1]
            val diffMinutes = if (endMinutes > startMinutes) endMinutes - startMinutes else 60
            diffMinutes * 60 * 1000L
        } catch (e: Exception) {
            3600000L // Default 1 hour duration
        }
    }
}
