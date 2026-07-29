package com.studentos.feature.attendance.di

import com.studentos.feature.attendance.data.repository.ClassEventRepositoryImpl
import com.studentos.feature.attendance.data.repository.SubjectRepositoryImpl
import com.studentos.feature.attendance.data.repository.TimetableRepositoryImpl
import com.studentos.feature.attendance.domain.repository.ClassEventRepository
import com.studentos.feature.attendance.domain.repository.SubjectRepository
import com.studentos.feature.attendance.domain.repository.TimetableRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AttendanceRepositoryModule — Hilt DI module binding repository interfaces to implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AttendanceRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTimetableRepository(
        impl: TimetableRepositoryImpl
    ): TimetableRepository

    @Binds
    @Singleton
    abstract fun bindSubjectRepository(
        impl: SubjectRepositoryImpl
    ): SubjectRepository

    @Binds
    @Singleton
    abstract fun bindClassEventRepository(
        impl: ClassEventRepositoryImpl
    ): ClassEventRepository
}
