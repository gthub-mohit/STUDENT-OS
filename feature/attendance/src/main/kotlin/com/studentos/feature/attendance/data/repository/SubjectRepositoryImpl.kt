package com.studentos.feature.attendance.data.repository

import com.studentos.core.database.dao.SubjectDao
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
    private val subjectDao: SubjectDao
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
}
