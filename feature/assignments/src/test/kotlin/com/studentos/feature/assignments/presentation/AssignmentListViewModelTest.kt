package com.studentos.feature.assignments.presentation

import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.AssignmentEntity
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.assignments.domain.model.AssignmentFilter
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.usecase.CreateAssignmentUseCase
import com.studentos.feature.assignments.domain.usecase.GetFilteredAssignmentsUseCase
import com.studentos.feature.assignments.domain.usecase.GetPrioritizedAssignmentsUseCase
import com.studentos.feature.assignments.domain.usecase.UpdateAssignmentStatusUseCase
import com.studentos.feature.assignments.presentation.state.AssignmentListUiState
import com.studentos.feature.assignments.presentation.viewmodel.AssignmentListViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssignmentListViewModelTest {

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
            listOf(SubjectEntity(id = 1L, name = "Physics"))
        )
        override fun getSubjectById(id: Long): Flow<SubjectEntity?> = error("Not needed")
        override suspend fun getByName(name: String): SubjectEntity? = error("Not needed")
        override suspend fun getByNames(names: List<String>): List<SubjectEntity> = emptyList()
        override suspend fun deleteById(id: Long) = error("Not needed")
        override suspend fun getSubjectCount(): Int = 1
    }

    private class FakeAssignmentRepository : AssignmentRepository {
        var deletedId: Long? = null
        var updatedStatus: String? = null

        override fun getAssignmentById(id: Long): Flow<AssignmentEntity?> = error("Not needed")
        override fun getAllAssignments(): Flow<List<AssignmentEntity>> = flowOf(
            listOf(
                AssignmentEntity(
                    id = 1L,
                    subjectId = 1L,
                    title = "Lab HW",
                    deadline = System.currentTimeMillis() + 86400000L,
                    status = AssignmentEntity.STATUS_PENDING,
                    createdAt = System.currentTimeMillis()
                )
            )
        )
        override fun getAssignmentsByStatus(status: String): Flow<List<AssignmentEntity>> = flowOf(emptyList())
        override fun getAssignmentsToday(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = flowOf(
            listOf(
                AssignmentEntity(
                    id = 1L,
                    subjectId = 1L,
                    title = "Lab HW",
                    deadline = System.currentTimeMillis() + 86400000L,
                    status = AssignmentEntity.STATUS_PENDING,
                    createdAt = System.currentTimeMillis()
                )
            )
        )
        override fun getAssignmentsThisWeek(startEpoch: Long, endEpoch: Long): Flow<List<AssignmentEntity>> = flowOf(emptyList())
        override fun getOverdueAssignments(nowEpoch: Long): Flow<List<AssignmentEntity>> = flowOf(emptyList())
        override suspend fun getUrgentAssignments(withinEpoch: Long): AppResult<List<AssignmentEntity>> = error("Not needed")

        override suspend fun createAssignment(assignment: AssignmentEntity): AppResult<Long> = AppResult.Success(1L)

        override suspend fun updateStatus(id: Long, newStatus: String): AppResult<Unit> {
            updatedStatus = newStatus
            return AppResult.Success(Unit)
        }

        override suspend fun updateDeadline(id: Long, deadline: Long): AppResult<Unit> = error("Not needed")
        override suspend fun updateReminderLead(id: Long, leadMs: Long?): AppResult<Unit> = error("Not needed")

        override suspend fun deleteAssignment(id: Long): AppResult<Unit> {
            deletedId = id
            return AppResult.Success(Unit)
        }

        override suspend fun setAttachment(id: Long, uri: String?): AppResult<Unit> = error("Not needed")
        override suspend fun attachFile(id: Long, sourceUriString: String): AppResult<String> = AppResult.Success("attachments/file.pdf")
    }

    @Test
    fun init_loadsAssignmentsWithCurrentFilterAndPrioritizedGroups() = runBlocking {
        val repo = FakeAssignmentRepository()
        val dao = FakeSubjectDao()
        val getUseCase = GetFilteredAssignmentsUseCase(repo)
        val prioritizeUseCase = GetPrioritizedAssignmentsUseCase(repo)
        val updateUseCase = UpdateAssignmentStatusUseCase(repo)
        val createUseCase = CreateAssignmentUseCase(repo)

        val viewModel = AssignmentListViewModel(getUseCase, prioritizeUseCase, updateUseCase, createUseCase, repo, dao)

        val state = viewModel.uiState.first()
        assertTrue(state is AssignmentListUiState.Success)
        val success = state as AssignmentListUiState.Success
        assertEquals(1, success.assignments.size)
        assertEquals("Lab HW", success.assignments[0].title)
        assertEquals("Physics", success.subjectsMap[1L])
        assertNotNull(success.prioritizedGroups)
    }

    @Test
    fun selectFilter_updatesSelectedFilter() = runBlocking {
        val repo = FakeAssignmentRepository()
        val dao = FakeSubjectDao()
        val getUseCase = GetFilteredAssignmentsUseCase(repo)
        val prioritizeUseCase = GetPrioritizedAssignmentsUseCase(repo)
        val updateUseCase = UpdateAssignmentStatusUseCase(repo)
        val createUseCase = CreateAssignmentUseCase(repo)

        val viewModel = AssignmentListViewModel(getUseCase, prioritizeUseCase, updateUseCase, createUseCase, repo, dao)

        viewModel.selectFilter(AssignmentFilter.OVERDUE)
        assertEquals(AssignmentFilter.OVERDUE, viewModel.selectedFilter.value)
    }
}
