package com.studentos.feature.settings.di

import com.studentos.feature.settings.data.repository.BackupRepositoryImpl
import com.studentos.feature.settings.data.repository.SettingsRepositoryImpl
import com.studentos.feature.settings.domain.repository.BackupRepository
import com.studentos.feature.settings.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * SettingsModule — Hilt module binding SettingsRepository and BackupRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: BackupRepositoryImpl
    ): BackupRepository
}
