package com.studentos.core.intelligence.di

import com.studentos.core.intelligence.provider.LLMProvider
import com.studentos.core.intelligence.provider.MockProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IntelligenceCoreModule {

    @Binds
    @Singleton
    abstract fun bindLLMProvider(impl: MockProvider): LLMProvider
}
