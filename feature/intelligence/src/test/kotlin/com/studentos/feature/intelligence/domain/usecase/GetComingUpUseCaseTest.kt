package com.studentos.feature.intelligence.domain.usecase

import com.studentos.feature.intelligence.domain.model.ComingUpItem
import com.studentos.feature.intelligence.domain.model.TodayFocusItem
import com.studentos.feature.intelligence.domain.repository.HomeOverviewRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetComingUpUseCaseTest {

    private val repository: HomeOverviewRepository = mockk()
    private val getTodayFocusUseCase: GetTodayFocusUseCase = mockk()
    private lateinit var getComingUpUseCase: GetComingUpUseCase

    @Before
    fun setUp() {
        getComingUpUseCase = GetComingUpUseCase(repository, getTodayFocusUseCase)
    }

    @Test
    fun sameEntityInTodayFocusAndComingUp_isExcludedFromComingUp() = runTest {
        // Today's focus has Assignment ID 10
        val todayFocus = listOf(
            TodayFocusItem(
                id = "asgn_10",
                title = "Finish Mechanics Assignment",
                subtitle = "Due today",
                category = "ASSIGNMENT",
                isCompleted = false,
                entityId = 10L
            )
        )
        // Coming up repository returns Assignment ID 10 and Assignment ID 11
        val comingUpRepoItems = listOf(
            ComingUpItem(
                id = "asgn_up_10",
                title = "Mechanics Assignment",
                subtitle = "Due today · 5:00 PM",
                category = "ASSIGNMENT",
                timestamp = 1000L,
                entityId = 10L
            ),
            ComingUpItem(
                id = "asgn_up_11",
                title = "Physics Assignment",
                subtitle = "Due tomorrow",
                category = "ASSIGNMENT",
                timestamp = 2000L,
                entityId = 11L
            )
        )

        every { getTodayFocusUseCase() } returns flowOf(todayFocus)
        every { repository.getComingUpItems() } returns flowOf(comingUpRepoItems)

        val result = getComingUpUseCase().first()

        // Assignment 10 must be excluded; only Assignment 11 remains
        assertEquals(1, result.size)
        assertEquals(11L, result[0].entityId)
        assertEquals("Physics Assignment", result[0].title)
    }

    @Test
    fun differentEntitiesWithSameTitle_bothRemainWhereAppropriate() = runTest {
        // Today's focus has Assignment ID 10 with title "Math Homework"
        val todayFocus = listOf(
            TodayFocusItem(
                id = "asgn_10",
                title = "Finish Math Homework",
                subtitle = "Part 1 · Due today",
                category = "ASSIGNMENT",
                isCompleted = false,
                entityId = 10L
            )
        )
        // Coming up has Assignment ID 20 (Part 2) with the same base title "Math Homework"
        val comingUpRepoItems = listOf(
            ComingUpItem(
                id = "asgn_up_20",
                title = "Math Homework",
                subtitle = "Part 2 · Due next week",
                category = "ASSIGNMENT",
                timestamp = 5000L,
                entityId = 20L
            )
        )

        every { getTodayFocusUseCase() } returns flowOf(todayFocus)
        every { repository.getComingUpItems() } returns flowOf(comingUpRepoItems)

        val result = getComingUpUseCase().first()

        // Assignment 20 has a different entity ID -> must NOT be excluded
        assertEquals(1, result.size)
        assertEquals(20L, result[0].entityId)
        assertEquals("Math Homework", result[0].title)
    }

    @Test
    fun tomorrowItem_remainsInComingUp() = runTest {
        val todayFocus = listOf(
            TodayFocusItem(
                id = "class_1",
                title = "Attend Operating Systems",
                subtitle = "10:00 AM",
                category = "ATTENDANCE",
                isCompleted = false,
                entityId = 1L
            )
        )
        val comingUpRepoItems = listOf(
            ComingUpItem(
                id = "asgn_up_100",
                title = "Chemistry Lab Report",
                subtitle = "Due tomorrow",
                category = "ASSIGNMENT",
                timestamp = 86400000L,
                entityId = 100L
            )
        )

        every { getTodayFocusUseCase() } returns flowOf(todayFocus)
        every { repository.getComingUpItems() } returns flowOf(comingUpRepoItems)

        val result = getComingUpUseCase().first()

        assertEquals(1, result.size)
        assertEquals(100L, result[0].entityId)
        assertEquals("Chemistry Lab Report", result[0].title)
    }

    @Test
    fun futureItem_remainsInComingUp() = runTest {
        val todayFocus = listOf(
            TodayFocusItem(
                id = "dsa_5",
                title = "Practice DSA: Binary Trees",
                subtitle = "Confidence: 3/5",
                category = "DSA",
                isCompleted = false,
                entityId = 5L
            )
        )
        val comingUpRepoItems = listOf(
            ComingUpItem(
                id = "contest_up_50",
                title = "Codeforces Round 950",
                subtitle = "Saturday · 8:00 PM",
                category = "CONTEST",
                timestamp = 170000000000L,
                entityId = 50L
            ),
            ComingUpItem(
                id = "proj_up_7",
                title = "Compiler Milestone 2",
                subtitle = "Next Friday",
                category = "PROJECT",
                timestamp = 180000000000L,
                entityId = 7L
            )
        )

        every { getTodayFocusUseCase() } returns flowOf(todayFocus)
        every { repository.getComingUpItems() } returns flowOf(comingUpRepoItems)

        val result = getComingUpUseCase().first()

        assertEquals(2, result.size)
        assertEquals("Codeforces Round 950", result[0].title)
        assertEquals("Compiler Milestone 2", result[1].title)
    }

    @Test
    fun todayActionableClassEvent_inTodayFocus_isExcludedFromComingUp() = runTest {
        // Today's Focus has Class Event ID 42
        val todayFocus = listOf(
            TodayFocusItem(
                id = "class_42",
                title = "Attend Data Structures",
                subtitle = "9:00 AM",
                category = "ATTENDANCE",
                isCompleted = false,
                entityId = 42L
            )
        )
        // Coming up repository returns Class Event 42 (e.g. today's occurrence) and Class Event 43 (tomorrow)
        val comingUpRepoItems = listOf(
            ComingUpItem(
                id = "class_up_42",
                title = "Data Structures",
                subtitle = "today · 9:00 AM",
                category = "CLASS",
                timestamp = 1000L,
                entityId = 42L
            ),
            ComingUpItem(
                id = "class_up_43",
                title = "Data Structures",
                subtitle = "tomorrow · 9:00 AM",
                category = "CLASS",
                timestamp = 2000L,
                entityId = 43L
            )
        )

        every { getTodayFocusUseCase() } returns flowOf(todayFocus)
        every { repository.getComingUpItems() } returns flowOf(comingUpRepoItems)

        val result = getComingUpUseCase().first()

        // Class event 42 (in Today's focus) must be excluded; Class event 43 (tomorrow) must remain
        assertEquals(1, result.size)
        assertEquals(43L, result[0].entityId)
        assertEquals("tomorrow · 9:00 AM", result[0].subtitle)
    }

    @Test
    fun emptyTodayFocus_comingUpBehavesExactlyAsBefore() = runTest {
        val comingUpRepoItems = listOf(
            ComingUpItem(
                id = "asgn_up_1",
                title = "Assignment 1",
                subtitle = "Due tomorrow",
                category = "ASSIGNMENT",
                timestamp = 1000L,
                entityId = 1L
            ),
            ComingUpItem(
                id = "class_up_2",
                title = "Class 2",
                subtitle = "Due tomorrow",
                category = "CLASS",
                timestamp = 2000L,
                entityId = 2L
            )
        )

        every { getTodayFocusUseCase() } returns flowOf(emptyList())
        every { repository.getComingUpItems() } returns flowOf(comingUpRepoItems)

        val result = getComingUpUseCase().first()

        assertEquals(2, result.size)
        assertEquals("Assignment 1", result[0].title)
        assertEquals("Class 2", result[1].title)
    }

    @Test
    fun assignmentInTodayFocus_doesNotExcludeClassWithSameNumericIdInComingUp() = runTest {
        val todayFocus = listOf(
            TodayFocusItem(
                id = "asgn_1",
                title = "Finish Math",
                subtitle = "Due today",
                category = "ASSIGNMENT",
                isCompleted = false,
                entityId = 1L
            )
        )
        val comingUpRepoItems = listOf(
            ComingUpItem(
                id = "class_up_1",
                title = "Math Class",
                subtitle = "tomorrow · 10:00 AM",
                category = "CLASS",
                timestamp = 2000L,
                entityId = 1L
            )
        )

        every { getTodayFocusUseCase() } returns flowOf(todayFocus)
        every { repository.getComingUpItems() } returns flowOf(comingUpRepoItems)

        val result = getComingUpUseCase().first()

        // Class event 1 must NOT be excluded by Assignment 1
        assertEquals(1, result.size)
        assertEquals(1L, result[0].entityId)
        assertEquals("Math Class", result[0].title)
        assertEquals("CLASS", result[0].category)
    }

    @Test
    fun newTaskWithTypeOther_appearsInComingUp() = runTest {
        val todayFocus = listOf(
            TodayFocusItem(
                id = "class_1",
                title = "Attend CS101",
                subtitle = "10:00 AM",
                category = "CLASS",
                isCompleted = false,
                entityId = 1L
            )
        )
        val comingUpRepoItems = listOf(
            ComingUpItem(
                id = "asgn_up_99",
                title = "Club Registration Form",
                subtitle = "Due Friday",
                category = "OTHER",
                timestamp = 50000L,
                entityId = 99L
            )
        )

        every { getTodayFocusUseCase() } returns flowOf(todayFocus)
        every { repository.getComingUpItems() } returns flowOf(comingUpRepoItems)

        val result = getComingUpUseCase().first()

        assertEquals(1, result.size)
        assertEquals(99L, result[0].entityId)
        assertEquals("Club Registration Form", result[0].title)
        assertEquals("OTHER", result[0].category)
    }
}
