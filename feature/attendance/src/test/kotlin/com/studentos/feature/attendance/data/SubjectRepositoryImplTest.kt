package com.studentos.feature.attendance.data

import app.cash.turbine.test
import com.studentos.core.database.dao.SubjectDao
import com.studentos.core.database.entity.SubjectEntity
import com.studentos.core.events.AppResult
import com.studentos.feature.attendance.data.repository.SubjectRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubjectRepositoryImplTest {

    private val subjectDao: SubjectDao = mockk(relaxed = true)
    private lateinit var repository: SubjectRepositoryImpl

    @Before
    fun setUp() {
        repository = SubjectRepositoryImpl(subjectDao)
    }

    @Test
    fun getActiveSubjects_returnsFlowFromDao() = runTest {
        val subject = SubjectEntity(id = 1L, name = "Physics")
        every { subjectDao.getActiveSubjects() } returns flowOf(listOf(subject))

        repository.getActiveSubjects().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Physics", list[0].name)
            awaitComplete()
        }
    }

    @Test
    fun addSubject_validatesNameAndInsertsDao() = runTest {
        coEvery { subjectDao.getByName("Chemistry") } returns null
        coEvery { subjectDao.insert(any()) } returns 5L

        val result = repository.addSubject("  Chemistry  ")

        assertTrue(result is AppResult.Success)
        assertEquals(5L, (result as AppResult.Success).data)
        coVerify {
            subjectDao.insert(match { it.name == "Chemistry" })
        }
    }

    @Test
    fun addSubject_returnsFailureWhenSubjectNameIsEmpty() = runTest {
        val result = repository.addSubject("   ")

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun renameSubject_validatesNameAndCallsDaoRename() = runTest {
        val result = repository.renameSubject(1L, "Quantum Mechanics")

        assertTrue(result is AppResult.Success)
        coVerify {
            subjectDao.rename(1L, "Quantum Mechanics")
        }
    }

    @Test
    fun archiveSubject_callsDaoArchive() = runTest {
        val result = repository.archiveSubject(1L)

        assertTrue(result is AppResult.Success)
        coVerify {
            subjectDao.archive(1L, any())
        }
    }
}
