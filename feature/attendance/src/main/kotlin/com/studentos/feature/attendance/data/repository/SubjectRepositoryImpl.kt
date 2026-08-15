package com.studentos.feature.attendance.data.repository

import androidx.room.withTransaction
import com.studentos.core.database.AppDatabase
import com.studentos.core.database.dao.ClassEventDao
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.dao.TimetableSlotDao
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.events.AppError
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * SubjectRepositoryImpl — Data repository implementing [SubjectRepository].
 */
class SubjectRepositoryImpl @Inject constructor(
    private val subjectDao: SubjectDao,
    private val timetableSlotDao: TimetableSlotDao? = null,
    private val classEventDao: ClassEventDao? = null,
    private val database: AppDatabase? = null
) : SubjectRepository {

    override fun getActiveSubjects(): Flow<List<SubjectEntity>> {
        return subjectDao.getActiveSubjects()
    }

    override fun getAllSubjectsIncludingArchived(): Flow<List<SubjectEntity>> {
        return subjectDao.getAllSubjectsIncludingArchived()
    }

    override fun getSubjectById(id: Long): Flow<SubjectEntity?> {
        return subjectDao.getSubjectById(id)
    }

    override suspend fun addSubject(name: String): AppResult<Long> {
        return try {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return AppResult.Failure(AppError.ValidationError("Subject name cannot be empty"))
            }
            val existing = subjectDao.getByName(trimmedName)
            if (existing != null) {
                return AppResult.Failure(AppError.ValidationError("Subject already exists"))
            }
            val newEntity = SubjectEntity(name = trimmedName)
            val newId = subjectDao.insert(newEntity)
            AppResult.Success(newId)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to add subject"))
        }
    }

    override suspend fun renameSubject(id: Long, newName: String): AppResult<Unit> {
        return try {
            val trimmedName = newName.trim()
            if (trimmedName.isBlank()) {
                return AppResult.Failure(AppError.ValidationError("Subject name cannot be empty"))
            }
            subjectDao.rename(id, trimmedName)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to rename subject"))
        }
    }

    override suspend fun archiveSubject(id: Long): AppResult<Unit> {
        return try {
            val now = System.currentTimeMillis()
            subjectDao.archive(id, now)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to archive subject"))
        }
    }

    override suspend fun cleanupInvalidOcrSubjects(targetNames: List<String>): AppResult<Unit> {
        return try {
            val executeCleanup = suspend {
                val invalidSubjects = subjectDao.getByNames(targetNames)
                val now = System.currentTimeMillis()
                for (subject in invalidSubjects) {
                    val slotIds = timetableSlotDao?.getSlotIdsForSubject(subject.id) ?: emptyList()
                    if (slotIds.isNotEmpty()) {
                        classEventDao?.deleteUnmarkedBySlotIds(slotIds)
                        classEventDao?.nullifySlotReferences(slotIds, now)
                        timetableSlotDao?.deleteBySubjectId(subject.id)
                    }
                    classEventDao?.deleteUnmarkedBySubjectId(subject.id)
                    val remainingEvents = classEventDao?.countEventsForSubject(subject.id) ?: 0
                    if (remainingEvents == 0) {
                        subjectDao.deleteById(subject.id)
                    } else {
                        subjectDao.archive(subject.id, now)
                    }
                }
            }

            if (database != null) {
                database.withTransaction {
                    executeCleanup()
                }
            } else {
                executeCleanup()
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to cleanup invalid subjects"))
        }
    }
}
