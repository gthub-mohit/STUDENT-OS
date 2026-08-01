package com.studentos.feature.coding.usecase

import com.studentos.core.events.AppEvent
import com.studentos.core.events.AppEventBus
import com.studentos.feature.coding.domain.model.CpContest
import com.studentos.feature.coding.domain.model.CpProfile
import com.studentos.feature.coding.domain.model.CpReflection
import com.studentos.feature.coding.domain.repository.CpRepository
import com.studentos.feature.coding.domain.usecase.SaveContestReflectionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SaveContestReflectionUseCaseTest {

    private class FakeAppEventBus : AppEventBus {
        val emittedEvents = mutableListOf<AppEvent>()
        private val _events = MutableSharedFlow<AppEvent>(replay = 10)
        override val events: SharedFlow<AppEvent> = _events.asSharedFlow()

        override suspend fun emit(event: AppEvent) {
            emittedEvents.add(event)
            _events.emit(event)
        }
    }

    private class FakeCpRepository : CpRepository {
        var savedReflection: CpReflection? = null

        override fun getProfiles(): Flow<List<CpProfile>> = flowOf(emptyList())
        override fun getContests(profileId: Long): Flow<List<CpContest>> = flowOf(emptyList())
        override fun getAllContests(): Flow<List<CpContest>> = flowOf(emptyList())
        override fun getReflection(contestId: Long): Flow<CpReflection?> = flowOf(savedReflection)

        override suspend fun saveReflection(reflection: CpReflection) {
            savedReflection = reflection
        }
    }

    private lateinit var repository: FakeCpRepository
    private lateinit var eventBus: FakeAppEventBus
    private lateinit var useCase: SaveContestReflectionUseCase

    @Before
    fun setUp() {
        repository = FakeCpRepository()
        eventBus = FakeAppEventBus()
        useCase = SaveContestReflectionUseCase(repository, eventBus)
    }

    @Test
    fun invoke_validReflection_savesToRepositoryAndEmitsEvent() = runTest {
        val reflection = CpReflection(
            contestId = 42L,
            wentWrong = "Time limit exceeded",
            toRevise = "Segment Trees",
            selfRating = 4
        )

        useCase(reflection)

        assertNotNull(repository.savedReflection)
        assertEquals(42L, repository.savedReflection?.contestId)
        assertEquals("Time limit exceeded", repository.savedReflection?.wentWrong)
        assertEquals(4, repository.savedReflection?.selfRating)

        assertEquals(1, eventBus.emittedEvents.size)
        val event = eventBus.emittedEvents[0]
        assertTrue(event is AppEvent.ContestReflectionAdded)
        assertEquals(42L, (event as AppEvent.ContestReflectionAdded).contestId)
    }

    @Test
    fun invoke_invalidSelfRating_coercesRatingBetween1And5() = runTest {
        val reflectionTooHigh = CpReflection(contestId = 1L, selfRating = 10)
        useCase(reflectionTooHigh)
        assertEquals(5, repository.savedReflection?.selfRating)

        val reflectionTooLow = CpReflection(contestId = 2L, selfRating = -2)
        useCase(reflectionTooLow)
        assertEquals(1, repository.savedReflection?.selfRating)
    }
}
