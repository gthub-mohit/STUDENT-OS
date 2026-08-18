package com.studentos.feature.assignments.repository

import android.content.Context
import com.studentos.core.database.dao.AssignmentDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.data.repository.AssignmentRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class AssignmentRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var filesDir: File

    private class FakeAppEventBus : AppEventBus {
        private val _events = MutableSharedFlow<AppEvent>(replay = 10)
        override val events: SharedFlow<AppEvent> = _events.asSharedFlow()

        override suspend fun emit(event: AppEvent) {
            _events.emit(event)
        }
    }

    private class FakeAssignmentDao(
        var assignment: AssignmentEntity? = null
    ) : AssignmentDao {
        var lastInserted: AssignmentEntity? = null
        var lastUpdatedStatus: String? = null
        var lastDeletedId: Long? = null
        var lastUpdatedEntity: AssignmentEntity? = null

        override suspend fun insert(assignment: AssignmentEntity): Long {
            lastInserted = assignment
            this.assignment = assignment.copy(id = 42L)
            return 42L
        }

        override suspend fun update(assignment: AssignmentEntity) {
            lastUpdatedEntity = assignment
            this.assignment = assignment
        }

        override suspend fun updateStatus(id: Long, status: String, updatedAt: Long) {
            lastUpdatedStatus = status
        }

        override suspend fun updateDeadline(id: Long, deadline: Long, updatedAt: Long) {}
        override suspend fun updateReminderLeadMs(id: Long, reminderLeadMs: Long?, updatedAt: Long) {}

        override suspend fun deleteById(id: Long) {
            lastDeletedId = id
        }

        override fun getAllAssignments(): Flow<List<AssignmentEntity>> = flowOf(emptyList())
        override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> = flowOf(emptyList())
        override fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = flowOf(emptyList())
        override fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = flowOf(emptyList())
        override fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>> = flowOf(emptyList())
        override fun getAssignmentById(id: Long): Flow<AssignmentEntity?> = flowOf(assignment)
        override suspend fun getUrgentAssignments(withinEpoch: Long): List<AssignmentEntity> = emptyList()
    }

    private class MinimalTestContext(private val baseFilesDir: File) : android.content.ContextWrapper(null) {
        override fun getFilesDir(): File = baseFilesDir
        override fun getApplicationContext(): Context = this
    }

    @Before
    fun setUp() {
        filesDir = tempFolder.newFolder("filesDir")
    }

    @Test
    fun createAssignment_emitsAssignmentCreatedEvent() = runBlocking {
        val dao = FakeAssignmentDao()
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context)

        val newAssignment = AssignmentEntity(
            subjectId = 1L,
            title = "Math Homework",
            deadline = System.currentTimeMillis() + 86400000L,
            createdAt = System.currentTimeMillis()
        )

        val result = repository.createAssignment(newAssignment)
        assertTrue(result is AppResult.Success)
        assertEquals(42L, (result as AppResult.Success).data)

        val event = bus.events.first()
        assertTrue(event is AppEvent.AssignmentCreated)
        assertEquals(42L, (event as AppEvent.AssignmentCreated).assignmentId)
    }

    @Test
    fun updateStatus_emitsAssignmentStatusChangedEvent() = runBlocking {
        val initialAssignment = AssignmentEntity(
            id = 42L,
            subjectId = 1L,
            title = "Physics Lab",
            deadline = System.currentTimeMillis() + 86400000L,
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = System.currentTimeMillis()
        )
        val dao = FakeAssignmentDao(assignment = initialAssignment)
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context)

        val result = repository.updateStatus(42L, AssignmentEntity.STATUS_SUBMITTED)
        assertTrue(result is AppResult.Success)

        val event = bus.events.first()
        assertTrue(event is AppEvent.AssignmentStatusChanged)
        val statusEvent = event as AppEvent.AssignmentStatusChanged
        assertEquals(42L, statusEvent.assignmentId)
        assertEquals(AssignmentEntity.STATUS_SUBMITTED, statusEvent.newStatus)
    }

    @Test
    fun deleteAssignment_emitsAssignmentDeletedEvent() = runBlocking {
        val initialAssignment = AssignmentEntity(
            id = 42L,
            subjectId = 1L,
            title = "Attachment HW",
            deadline = System.currentTimeMillis() + 86400000L,
            createdAt = System.currentTimeMillis()
        )
        val dao = FakeAssignmentDao(assignment = initialAssignment)
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context)

        val result = repository.deleteAssignment(42L)
        assertTrue(result is AppResult.Success)

        val event = bus.events.first()
        assertTrue(event is AppEvent.AssignmentDeleted)
        assertEquals(42L, (event as AppEvent.AssignmentDeleted).assignmentId)
    }

    @Test
    fun attachFile_successfulImport_copiesFileToAttachmentsAndStoresRelativePath() = runBlocking {
        val initialAssignment = AssignmentEntity(
            id = 42L,
            subjectId = 1L,
            title = "Attachment HW",
            deadline = System.currentTimeMillis() + 86400000L,
            createdAt = System.currentTimeMillis()
        )
        val dao = FakeAssignmentDao(assignment = initialAssignment)
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context).apply {
            openInputStreamProvider = { _, _ -> ByteArrayInputStream("Test file content".toByteArray()) }
            mimeTypeProvider = { _, _ -> "application/pdf" }
        }

        val result = repository.attachFile(42L, "content://valid/path.pdf")
        assertTrue(result is AppResult.Success)

        val relativePath = (result as AppResult.Success).data
        assertTrue(relativePath.startsWith("attachments/"))
        assertTrue(relativePath.endsWith(".pdf"))

        val createdFile = File(filesDir, relativePath)
        assertTrue(createdFile.exists())
        assertEquals("Test file content", createdFile.readText())

        assertEquals(relativePath, dao.lastUpdatedEntity?.attachmentUri)
    }

    @Test
    fun attachFile_replacement_deletesOldFile() = runBlocking {
        val attachmentsDir = File(filesDir, "attachments").apply { mkdirs() }
        val oldFile = File(attachmentsDir, "old_file.pdf").apply { writeText("Old content") }

        val initialAssignment = AssignmentEntity(
            id = 42L,
            subjectId = 1L,
            title = "Attachment HW",
            deadline = System.currentTimeMillis() + 86400000L,
            attachmentUri = "attachments/old_file.pdf",
            createdAt = System.currentTimeMillis()
        )
        val dao = FakeAssignmentDao(assignment = initialAssignment)
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context).apply {
            openInputStreamProvider = { _, _ -> ByteArrayInputStream("New file content".toByteArray()) }
            mimeTypeProvider = { _, _ -> "application/pdf" }
        }

        val result = repository.attachFile(42L, "content://valid/new_path.pdf")
        assertTrue(result is AppResult.Success)

        assertFalse(oldFile.exists())

        val newRelativePath = (result as AppResult.Success).data
        assertTrue(File(filesDir, newRelativePath).exists())
    }

    @Test
    fun setAttachment_nullRemoval_deletesLocalFile() = runBlocking {
        val attachmentsDir = File(filesDir, "attachments").apply { mkdirs() }
        val oldFile = File(attachmentsDir, "to_delete.pdf").apply { writeText("Delete me") }

        val initialAssignment = AssignmentEntity(
            id = 42L,
            subjectId = 1L,
            title = "Attachment HW",
            deadline = System.currentTimeMillis() + 86400000L,
            attachmentUri = "attachments/to_delete.pdf",
            createdAt = System.currentTimeMillis()
        )
        val dao = FakeAssignmentDao(assignment = initialAssignment)
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context)

        val result = repository.setAttachment(42L, null)
        assertTrue(result is AppResult.Success)
        assertFalse(oldFile.exists())
        assertNull(dao.lastUpdatedEntity?.attachmentUri)
    }

    @Test
    fun deleteAssignment_deletesOrphanedLocalFile() = runBlocking {
        val attachmentsDir = File(filesDir, "attachments").apply { mkdirs() }
        val fileToDelete = File(attachmentsDir, "assignment_file.pdf").apply { writeText("File content") }

        val initialAssignment = AssignmentEntity(
            id = 42L,
            subjectId = 1L,
            title = "Attachment HW",
            deadline = System.currentTimeMillis() + 86400000L,
            attachmentUri = "attachments/assignment_file.pdf",
            createdAt = System.currentTimeMillis()
        )
        val dao = FakeAssignmentDao(assignment = initialAssignment)
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context)

        val result = repository.deleteAssignment(42L)
        assertTrue(result is AppResult.Success)
        assertFalse(fileToDelete.exists())
        assertEquals(42L, dao.lastDeletedId)
    }

    @Test
    fun attachFile_streamCopyFailure_returnsFailureResult() = runBlocking {
        val initialAssignment = AssignmentEntity(
            id = 42L,
            subjectId = 1L,
            title = "Attachment HW",
            deadline = System.currentTimeMillis() + 86400000L,
            createdAt = System.currentTimeMillis()
        )
        val dao = FakeAssignmentDao(assignment = initialAssignment)
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context).apply {
            openInputStreamProvider = { _, _ -> null }
        }

        val result = repository.attachFile(42L, "content://invalid/stream")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun createAssignment_missingDeadline_returnsValidationError() = runBlocking {
        val dao = FakeAssignmentDao()
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context)

        val newAssignment = AssignmentEntity(
            subjectId = 1L,
            title = "Math Quiz",
            deadline = 0L,
            createdAt = System.currentTimeMillis(),
            taskType = "QUIZ"
        )

        val result = repository.createAssignment(newAssignment)
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun createAssignment_withCustomTaskType_persistsTaskType() = runBlocking {
        val dao = FakeAssignmentDao()
        val bus = FakeAppEventBus()
        val context = MinimalTestContext(filesDir)
        val repository = AssignmentRepositoryImpl(dao, bus, context)

        val newAssignment = AssignmentEntity(
            subjectId = 1L,
            title = "Compiler Lab Record",
            deadline = 1755500000000L,
            createdAt = System.currentTimeMillis(),
            taskType = "LAB_RECORD"
        )

        val result = repository.createAssignment(newAssignment)
        assertTrue(result is AppResult.Success)
        assertEquals("LAB_RECORD", dao.lastInserted?.taskType)
    }
}
