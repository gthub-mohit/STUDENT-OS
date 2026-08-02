package com.studentos.feature.assignments.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppError
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.scheduler.AssignmentReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

/**
 * AssignmentRepositoryImpl — Data repository implementing [AssignmentRepository].
 */
class AssignmentRepositoryImpl @Inject constructor(
    private val assignmentDao: AssignmentDao,
    private val eventBus: AppEventBus,
    @ApplicationContext private val context: Context,
    private val reminderScheduler: AssignmentReminderScheduler? = null
) : AssignmentRepository {

    var openInputStreamProvider: (Context, String) -> InputStream? = { ctx, uriStr ->
        try {
            ctx.contentResolver.openInputStream(Uri.parse(uriStr))
        } catch (_: Exception) {
            null
        }
    }

    var mimeTypeProvider: (Context, String) -> String? = { ctx, uriStr ->
        try {
            ctx.contentResolver.getType(Uri.parse(uriStr))
        } catch (_: Exception) {
            null
        }
    }

    override fun getAssignmentById(id: Long): Flow<AssignmentEntity?> {
        return assignmentDao.getAssignmentById(id)
    }

    override fun getAllAssignments(): Flow<List<AssignmentEntity>> {
        return assignmentDao.getAllAssignments()
    }

    override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> {
        return assignmentDao.getAssignmentsByStatus(status)
    }

    override fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> {
        return assignmentDao.getAssignmentsToday(startEpoch, endEpoch)
    }

    override fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> {
        return assignmentDao.getAssignmentsThisWeek(startEpoch, endEpoch)
    }

    override fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>> {
        return assignmentDao.getOverdueAssignments(nowEpoch)
    }

    override suspend fun getUrgentAssignments(withinEpoch: Long): AppResult<List<AssignmentEntity>> = withContext(Dispatchers.IO) {
        try {
            val list = assignmentDao.getUrgentAssignments(withinEpoch)
            AppResult.Success(list)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to fetch urgent assignments"))
        }
    }

    override suspend fun createAssignment(assignment: AssignmentEntity): AppResult<Long> = withContext(Dispatchers.IO) {
        try {
            if (assignment.title.isBlank()) {
                return@withContext AppResult.Failure(AppError.ValidationError("Assignment title cannot be empty"))
            }
            val newId = assignmentDao.insert(assignment)
            val created = assignment.copy(id = newId)
            reminderScheduler?.scheduleReminder(created)
            eventBus.emit(AppEvent.AssignmentCreated(assignmentId = newId))
            AppResult.Success(newId)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to create assignment"))
        }
    }

    override suspend fun updateStatus(id: Long, newStatus: String): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            assignmentDao.updateStatus(id, newStatus, now)
            eventBus.emit(AppEvent.AssignmentStatusChanged(assignmentId = id, newStatus = newStatus))
            
            val updated = assignmentDao.getAssignmentById(id).firstOrNull()
            if (updated != null) {
                reminderScheduler?.scheduleReminder(updated)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to update assignment status"))
        }
    }

    override suspend fun updateDeadline(id: Long, deadline: Long): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            assignmentDao.updateDeadline(id, deadline, now)
            
            val updated = assignmentDao.getAssignmentById(id).firstOrNull()
            if (updated != null) {
                reminderScheduler?.scheduleReminder(updated)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to update deadline"))
        }
    }

    override suspend fun updateReminderLead(id: Long, leadMs: Long?): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            assignmentDao.updateReminderLeadMs(id, leadMs, now)
            
            val updated = assignmentDao.getAssignmentById(id).firstOrNull()
            if (updated != null) {
                reminderScheduler?.scheduleReminder(updated)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to update reminder lead"))
        }
    }

    override suspend fun deleteAssignment(id: Long): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = assignmentDao.getAssignmentById(id).firstOrNull()
            assignmentDao.deleteById(id)

            // Cleanup local file attachment if present
            existing?.attachmentUri?.let { path ->
                deleteLocalFile(path)
            }

            reminderScheduler?.cancelReminder(id)
            eventBus.emit(AppEvent.AssignmentDeleted(assignmentId = id))
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to delete assignment"))
        }
    }

    override suspend fun setAttachment(id: Long, uri: String?): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = assignmentDao.getAssignmentById(id).firstOrNull()
                ?: return@withContext AppResult.Failure(AppError.ValidationError("Assignment not found"))

            val currentUri = existing.attachmentUri
            if (uri == null && currentUri != null) {
                deleteLocalFile(currentUri)
            }

            val updated = existing.copy(
                attachmentUri = uri,
                updatedAt = System.currentTimeMillis()
            )
            assignmentDao.update(updated)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to set attachment"))
        }
    }

    override suspend fun attachFile(id: Long, sourceUriString: String): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val existing = assignmentDao.getAssignmentById(id).firstOrNull()
                ?: return@withContext AppResult.Failure(AppError.ValidationError("Assignment not found"))

            val extension = getExtension(sourceUriString)
            val uuid = UUID.randomUUID().toString()
            val relativePath = "attachments/$uuid.$extension"

            val attachmentsDir = File(context.filesDir, "attachments")
            if (!attachmentsDir.exists()) {
                attachmentsDir.mkdirs()
            }

            val destFile = File(context.filesDir, relativePath)
            val inputStream = openInputStreamProvider(context, sourceUriString)
                ?: return@withContext AppResult.Failure(AppError.ValidationError("Could not open source file stream"))

            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Delete old attachment if present
            val oldPath = existing.attachmentUri
            if (oldPath != null) {
                deleteLocalFile(oldPath)
            }

            val updated = existing.copy(
                attachmentUri = relativePath,
                updatedAt = System.currentTimeMillis()
            )
            assignmentDao.update(updated)

            AppResult.Success(relativePath)
        } catch (e: Exception) {
            AppResult.Failure(AppError.DatabaseError(e.message ?: "Failed to copy attachment file"))
        }
    }

    private fun deleteLocalFile(pathString: String) {
        try {
            val file = if (pathString.startsWith("/")) {
                File(pathString)
            } else {
                File(context.filesDir, pathString)
            }
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignore file deletion errors
        }
    }

    private fun getExtension(sourceUriString: String): String {
        try {
            val type = mimeTypeProvider(context, sourceUriString)
            if (type != null) {
                val ext = MimeTypeMap.getSingleton()?.getExtensionFromMimeType(type)
                if (!ext.isNullOrEmpty()) return ext
            }
        } catch (_: Exception) {
            // MimeTypeMap is stubbed in non-Robolectric unit tests
        }
        val clean = sourceUriString.substringBefore("?").substringBefore("#")
        val lastSegment = clean.substringAfterLast("/")
        return if (lastSegment.contains(".")) {
            lastSegment.substringAfterLast(".")
        } else {
            "bin"
        }
    }
}
