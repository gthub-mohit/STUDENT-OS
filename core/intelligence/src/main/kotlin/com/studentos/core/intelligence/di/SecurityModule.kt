package com.studentos.core.intelligence.di

import android.content.Context
import com.studentos.core.intelligence.security.KeystoreCryptoManager
import com.studentos.core.intelligence.security.SecureCredentialStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecureCredentialStorage(
        @ApplicationContext context: Context
    ): SecureCredentialStorage {
        return KeystoreCryptoManager(context)
    }
}
