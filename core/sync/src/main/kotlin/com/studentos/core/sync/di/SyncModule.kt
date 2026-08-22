package com.studentos.core.sync.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.studentos.core.sync.api.CodeChefApiService
import com.studentos.core.sync.api.CodeforcesApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    @Named("cp")
    fun provideCpOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    @Named("cp")
    fun provideCpRetrofit(
        @Named("cp") okHttpClient: OkHttpClient
    ): Retrofit {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        return Retrofit.Builder()
            .baseUrl("https://codeforces.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideCodeChefApiService(
        impl: com.studentos.core.sync.api.CodeChefApiServiceImpl
    ): CodeChefApiService = impl

    @Provides
    @Singleton
    fun provideCodeforcesApiService(
        @Named("cp") retrofit: Retrofit
    ): CodeforcesApiService {
        return retrofit.create(CodeforcesApiService::class.java)
    }
}
