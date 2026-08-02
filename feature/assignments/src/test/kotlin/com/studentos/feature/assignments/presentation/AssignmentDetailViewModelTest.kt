package com.studentos.feature.assignments.presentation

import androidx.lifecycle.SavedStateHandle
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.usecase.UpdateAssignmentStatusUseCase
import com.studentos.feature.assignments.presentation.state.AssignmentDetailUiState
import com.studentos.feature.assignments.presentation.viewmodel.AssignmentDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssignmentDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeSubjectDao : SubjectDao {
        override suspend fun insert(subject: SubjectEntity): Long = error("Not needed")
        override suspend fun insertAll(subjects: List<SubjectEntity>): List<Long> = error("Not needed")
        override suspend fun update(subject: SubjectEntity) = error("Not needed")
        override suspend fun rename(id: Long, newName: String) = error("Not needed")
        override suspend fun archive(id: Long, archivedAt: Long) = error("Not needed")
        override fun getActiveSubjects(): Flow<List<SubjectEntity>> = error("Not needed")
        override fun getAllSubjectsIncludingArchived(): Flow<List<SubjectEntity>> = flowOf(
            listOf(SubjectEntity(id = 10L, name = "Software Architecture"))
        )
        override fun getSubjectById(id: Long): Flow<SubjectEntity?> = error("Not needed")
        override suspend fun getByName(name: String): SubjectEntity? = error("Not needed")
        override suspend fun getSubjectCount(): Int = 1
    }

    private class FakeAssignmentRepository(
        var assignment: AssignmentEntity? = null
    ) : AssignmentRepository {
        var updatedStatus: String? = null
        var deletedId: Long? = null
        var updatedAttachmentUri: String? = null
        var attachedSourceUri: String? = null

        override fun getAssignmentById(id: Long): Flow<AssignmentEntity?> = flowOf(assignment)
        override fun getAllAssignments(): Flow<List<AssignmentEntity>> = flowOf(emptyList())
        override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>> = error("Not needed")
        override suspend fun getUrgentAssignments(withinEpoch: Long): AppResult<List<AssignmentEntity>> = error("Not needed")
        override suspend fun createAssignment(assignment: AssignmentEntity): AppResult<Long> = error("Not needed")

        override suspend fun updateStatus(id: Long, newStatus: String): AppResult<Unit> {
            updatedStatus = newStatus
            return AppResult.Success(Unit)
        }

        override suspend fun updateDeadline(id: Long, deadline: Long): AppResult<Unit> = error("Not needed")
        override suspend fun updateReminderLead(id: Long, leadMs: Long?): AppResult<Unit> = error("Not needed")

        override suspend fun deleteAssignment(id: Long): AppResult<Unit> {
            deletedId = id
            assignment = null
            return AppResult.Success(Unit)
        }

        override suspend fun setAttachment(id: Long, uri: String?): AppResult<Unit> {
            updatedAttachmentUri = uri
            return AppResult.Success(Unit)
        }

        override suspend fun attachFile(id: Long, sourceUriString: String): AppResult<String> {
            attachedSourceUri = sourceUriString
            val relPath = "attachments/mock.pdf"
            assignment = assignment?.copy(attachmentUri = relPath)
            return AppResult.Success(relPath)
        }
    }

    @Test
    fun observeAssignment_validId_loadsAssignmentSuccess() = runBlocking {
        val initialAssignment = AssignmentEntity(
            id = 5L,
            subjectId = 10L,
            title = "Design Doc",
            deadline = System.currentTimeMillis() + 86400000L,
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = System.currentTimeMillis()
        )
        val repo = FakeAssignmentRepository(assignment = initialAssignment)
        val subjectDao = FakeSubjectDao()
        val updateStatusUseCase = UpdateAssignmentStatusUseCase(repo)
        val savedStateHandle = SavedStateHandle(mapOf("id" to "5"))

        val viewModel = AssignmentDetailViewModel(savedStateHandle, repo, updateStatusUseCase, subjectDao)

        val state = viewModel.uiState.first { it is AssignmentDetailUiState.Success } as AssignmentDetailUiState.Success
        assertEquals("Design Doc", state.assignment.title)
        assertEquals("Software Architecture", state.subjectName)
        assertFalse(state.showDeleteConfirmation)
    }

    @Test
    fun cycleStatus_pendingAssignment_updatesToInProgress() = runBlocking {
        val initialAssignment = AssignmentEntity(
            id = 5L,
            subjectId = 10L,
            title = "Design Doc",
            deadline = System.currentTimeMillis() + 86400000L,
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = System.currentTimeMillis()
        )
        val repo = FakeAssignmentRepository(assignment = initialAssignment)
        val subjectDao = FakeSubjectDao()
        val updateStatusUseCase = UpdateAssignmentStatusUseCase(repo)
        val savedStateHandle = SavedStateHandle(mapOf("id" to "5"))

        val viewModel = AssignmentDetailViewModel(savedStateHandle, repo, updateStatusUseCase, subjectDao)

        viewModel.cycleStatus()

        assertEquals(AssignmentEntity.STATUS_IN_PROGRESS, repo.updatedStatus)
    }

    @Test
    fun addAttachment_delegatesToRepositoryAttachFile() = runBlocking {
        val initialAssignment = AssignmentEntity(
            id = 5L,
            subjectId = 10L,
            title = "Design Doc",
            deadline = System.currentTimeMillis() + 86400000L,
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = System.currentTimeMillis()
        )
        val repo = FakeAssignmentRepository(assignment = initialAssignment)
        val subjectDao = FakeSubjectDao()
        val updateStatusUseCase = UpdateAssignmentStatusUseCase(repo)
        val savedStateHandle = SavedStateHandle(mapOf("id" to "5"))

        val viewModel = AssignmentDetailViewModel(savedStateHandle, repo, updateStatusUseCase, subjectDao)

        viewModel.addAttachment("content://test/file.pdf")

        assertEquals("content://test/file.pdf", repo.attachedSourceUri)
    }

    @Test
    fun requestDelete_pendingAssignment_showsConfirmationDialog() = runBlocking {
        val initialAssignment = AssignmentEntity(
            id = 5L,
            subjectId = 10L,
            title = "Design Doc",
            deadline = System.currentTimeMillis() + 86400000L,
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = System.currentTimeMillis()
        )
        val repo = FakeAssignmentRepository(assignment = initialAssignment)
        val subjectDao = FakeSubjectDao()
        val updateStatusUseCase = UpdateAssignmentStatusUseCase(repo)
        val savedStateHandle = SavedStateHandle(mapOf("id" to "5"))

        val viewModel = AssignmentDetailViewModel(savedStateHandle, repo, updateStatusUseCase, subjectDao)

        viewModel.requestDelete()

        val state = viewModel.uiState.first { (it as? AssignmentDetailUiState.Success)?.showDeleteConfirmation == true } as AssignmentDetailUiState.Success
        assertTrue(state.showDeleteConfirmation)

        viewModel.dismissDeleteDialog()
        val dismissedState = viewModel.uiState.first { (it as? AssignmentDetailUiState.Success)?.showDeleteConfirmation == false } as AssignmentDetailUiState.Success
        assertFalse(dismissedState.showDeleteConfirmation)
    }

    @Test
    fun confirmDelete_deletesAssignmentAndTransitionsToDeleted() = runBlocking {
        val initialAssignment = AssignmentEntity(
            id = 5L,
            subjectId = 10L,
            title = "Design Doc",
            deadline = System.currentTimeMillis() + 86400000L,
            status = AssignmentEntity.STATUS_PENDING,
            createdAt = System.currentTimeMillis()
        )
        val repo = FakeAssignmentRepository(assignment = initialAssignment)
        val subjectDao = FakeSubjectDao()
        val updateStatusUseCase = UpdateAssignmentStatusUseCase(repo)
        val savedStateHandle = SavedStateHandle(mapOf("id" to "5"))

        val viewModel = AssignmentDetailViewModel(savedStateHandle, repo, updateStatusUseCase, subjectDao)

        viewModel.confirmDelete()

        val state = viewModel.uiState.first { it is AssignmentDetailUiState.Deleted }
        assertEquals(AssignmentDetailUiState.Deleted, state)
        assertEquals(5L, repo.deletedId)
    }
}
