package com.studentos.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * AppModule — top-level Hilt module for the :app shell.
 *
 * Provides application-scoped bindings that do not belong to any core or
 * feature module. Examples include: Application context aliases, analytics
 * wrappers, or cross-cutting concerns that span multiple modules.
 *
 * Task 0.2: Stub — no bindings yet.
 * Task 0.3: HiltWorkerFactory wiring added here.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Task 0.3+: Add @Provides / @Binds methods here as needed.
}
