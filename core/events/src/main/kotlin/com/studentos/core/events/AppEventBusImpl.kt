package com.studentos.core.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * AppEventBusImpl — Implementation of [AppEventBus] backed by a SharedFlow.
 *
 * Configured with replay = 0 to prevent replay on new subscriptions (tasks.md Line 88).
 */
class AppEventBusImpl @Inject constructor() : AppEventBus {

    private val _events = MutableSharedFlow<AppEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    override suspend fun emit(event: AppEvent) {
        _events.emit(event)
    }
}
