package com.studentos.core.events

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppEventBusTest {

    private lateinit var eventBus: AppEventBus

    @Before
    fun setUp() {
        eventBus = AppEventBusImpl()
    }

    @Test
    fun emit_dispatchesEventToCollector() = runTest {
        val event = AppEvent.AttendanceMarked(subjectId = 1L, status = "PRESENT")

        eventBus.events.test {
            eventBus.emit(event)
            val received = awaitItem()
            assertEquals(event, received)
        }
    }

    @Test
    fun multipleCollectors_receiveEmittedEvents() = runTest {
        val event = AppEvent.AssignmentCreated(assignmentId = 42L)

        eventBus.events.test {
            eventBus.events.test {
                eventBus.emit(event)
                assertEquals(event, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(event, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noReplay_onNewSubscriptions() = runTest {
        val oldEvent = AppEvent.DsaTopicUpdated(topicId = 10L)
        val newEvent = AppEvent.CpSyncCompleted

        eventBus.emit(oldEvent)

        eventBus.events.test {
            eventBus.emit(newEvent)
            assertEquals(newEvent, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun eventsEmittedInOrder() = runTest {
        val event1 = AppEvent.AttendanceMarked(1L, "PRESENT")
        val event2 = AppEvent.AssignmentStatusChanged(2L, "SUBMITTED")
        val event3 = AppEvent.DailyScoreChanged(85)

        eventBus.events.test {
            eventBus.emit(event1)
            eventBus.emit(event2)
            eventBus.emit(event3)

            assertEquals(event1, awaitItem())
            assertEquals(event2, awaitItem())
            assertEquals(event3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
