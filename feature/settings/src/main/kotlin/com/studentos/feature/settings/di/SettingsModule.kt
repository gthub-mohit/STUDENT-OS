package com.studentos.feature.settings.di

import com.studentos.feature.settings.data.repository.SettingsRepositoryImpl
import com.studentos.feature.settings.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * SettingsModule — Hilt module binding SettingsRepository interface to SettingsRepositoryImpl.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
