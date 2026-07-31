package com.studentos.feature.coding.di

import com.studentos.feature.coding.data.repository.CpRepositoryImpl
import com.studentos.feature.coding.domain.repository.CpRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CodingModule {

    @Binds
    @Singleton
    abstract fun bindCpRepository(
        impl: CpRepositoryImpl
    ): CpRepository
}
