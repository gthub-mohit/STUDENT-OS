package com.studentos.feature.intelligence.di

import com.studentos.feature.intelligence.data.repository.DailyBriefRepositoryImpl
import com.studentos.feature.intelligence.domain.repository.DailyBriefRepository
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
}
