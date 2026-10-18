package com.studentos.feature.intelligence.domain

import com.studentos.core.database.dao.GoalDao
import com.studentos.core.database.entity.GoalEntity
import com.studentos.feature.intelligence.domain.usecase.SaveGoalUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveGoalUseCaseTest {

    private val goalDao: GoalDao = mockk()
    private lateinit var useCase: SaveGoalUseCase

    @Before
    fun setup() {
        useCase = SaveGoalUseCase(goalDao)
    }

    @Test
    fun `validate catches blank title`() {
        val validation = useCase.validate(
            title = "   ",
            category = "DSA",
            metricType = "COUNT",
            unit = "questions",
            startValue = 0.0,
            targetValue = 100.0,
            startDate = 1000L,
            targetDate = 5000L
        )

        assertFalse(validation.isValid)
        assertTrue(validation.titleError?.contains("title cannot be empty") == true)
    }

    @Test
    fun `validate catches target date before or equal to start date`() {
        val validation = useCase.validate(
            title = "Valid Title",
            category = "DSA",
            metricType = "COUNT",
            unit = "questions",
            startValue = 0.0,
            targetValue = 100.0,
            startDate = 5000L,
            targetDate = 2000L
        )

        assertFalse(validation.isValid)
        assertTrue(validation.dateError?.contains("Target deadline must be after") == true)
    }

    @Test
    fun `validate catches target value less than or equal to start value`() {
        val validation = useCase.validate(
            title = "Valid Title",
            category = "DSA",
            metricType = "COUNT",
            unit = "questions",
            startValue = 50.0,
            targetValue = 30.0,
            startDate = 1000L,
            targetDate = 5000L
        )

        assertFalse(validation.isValid)
        assertTrue(validation.valueError?.contains("Target value must be greater") == true)
    }

    @Test
    fun `valid goal saves successfully to GoalDao`() = runTest {
        val goalSlot = slot<GoalEntity>()
        coEvery { goalDao.insert(capture(goalSlot)) } returns 42L

        val result = useCase(
            id = 0L,
            title = "Complete 50 Project Hours",
            category = "PROJECT",
            metricType = "HOURS",
            unit = "hours",
            startValue = 5.0,
            targetValue = 50.0,
            startDate = 1000L,
            targetDate = 10000L,
            isArchived = false
        )

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrThrow())

        val saved = goalSlot.captured
        assertEquals("Complete 50 Project Hours", saved.title)
        assertEquals("PROJECT", saved.category)
        assertEquals("HOURS", saved.metricType)
        assertEquals("hours", saved.unit)
        assertEquals(5.0, saved.startValue, 0.001)
        assertEquals(50.0, saved.targetValue, 0.001)
        coVerify(exactly = 1) { goalDao.insert(any()) }
    }
}
