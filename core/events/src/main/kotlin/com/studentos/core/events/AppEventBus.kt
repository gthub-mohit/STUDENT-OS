package com.studentos.core.events

import kotlinx.coroutines.flow.SharedFlow

/**
 * AppEventBus — Asynchronous event bus interface for broadcasting [AppEvent] system events.
 */
interface AppEventBus {
    val events: SharedFlow<AppEvent>
    suspend fun emit(event: AppEvent)
}
