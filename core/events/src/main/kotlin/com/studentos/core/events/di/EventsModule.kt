package com.studentos.core.events.di

import com.studentos.core.events.AppEventBus
import com.studentos.core.events.AppEventBusImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * EventsModule — Hilt DI module binding AppEventBusImpl to AppEventBus as a Singleton.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EventsModule {

    @Binds
    @Singleton
    abstract fun bindAppEventBus(impl: AppEventBusImpl): AppEventBus
}
