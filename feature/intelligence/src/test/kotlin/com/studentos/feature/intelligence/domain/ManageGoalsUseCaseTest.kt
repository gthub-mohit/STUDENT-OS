package com.studentos.feature.intelligence.domain

import com.studentos.core.database.dao.GoalDao
import com.studentos.feature.intelligence.domain.usecase.ManageGoalsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManageGoalsUseCaseTest {

    private val goalDao: GoalDao = mockk()
    private lateinit var useCase: ManageGoalsUseCase

    @Before
    fun setup() {
        useCase = ManageGoalsUseCase(goalDao)
    }

    @Test
    fun `setArchived updates archive flag in GoalDao`() = runTest {
        coEvery { goalDao.setArchived(10L, true, any()) } returns Unit

        val result = useCase.setArchived(10L, true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { goalDao.setArchived(10L, true, any()) }
    }

    @Test
    fun `deleteGoal deletes by id in GoalDao`() = runTest {
        coEvery { goalDao.deleteById(15L) } returns Unit

        val result = useCase.deleteGoal(15L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { goalDao.deleteById(15L) }
    }
}
