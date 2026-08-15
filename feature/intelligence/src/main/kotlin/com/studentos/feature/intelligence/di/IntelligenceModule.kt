package com.studentos.feature.intelligence.di

import com.studentos.feature.intelligence.data.repository.DailyBriefRepositoryImpl
import com.studentos.feature.intelligence.data.repository.FocusAssignmentRepositoryImpl
import com.studentos.feature.intelligence.data.repository.FocusAttendanceRepositoryImpl
import com.studentos.feature.intelligence.data.repository.FocusDsaRepositoryImpl
import com.studentos.feature.intelligence.data.repository.FocusProjectRepositoryImpl
import com.studentos.feature.intelligence.data.repository.HomeOverviewRepositoryImpl
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
import com.studentos.feature.intelligence.domain.repository.FocusAssignmentRepository
import com.studentos.feature.intelligence.domain.repository.FocusAttendanceRepository
import com.studentos.feature.intelligence.domain.repository.FocusDsaRepository
import com.studentos.feature.intelligence.domain.repository.FocusProjectRepository
import com.studentos.feature.intelligence.domain.repository.HomeOverviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IntelligenceModule {

    @Binds
    @Singleton
    abstract fun bindDailyBriefRepository(
        impl: DailyBriefRepositoryImpl
    ): DailyBriefRepository

    @Binds
    @Singleton
    abstract fun bindHomeOverviewRepository(
        impl: HomeOverviewRepositoryImpl
    ): HomeOverviewRepository

    @Binds
    @Singleton
    abstract fun bindFocusAssignmentRepository(
        impl: FocusAssignmentRepositoryImpl
    ): FocusAssignmentRepository

    @Binds
    @Singleton
    abstract fun bindFocusAttendanceRepository(
        impl: FocusAttendanceRepositoryImpl
    ): FocusAttendanceRepository

    @Binds
    @Singleton
    abstract fun bindFocusDsaRepository(
        impl: FocusDsaRepositoryImpl
    ): FocusDsaRepository

    @Binds
    @Singleton
    abstract fun bindFocusProjectRepository(
        impl: FocusProjectRepositoryImpl
    ): FocusProjectRepository
}
