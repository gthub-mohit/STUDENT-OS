package com.studentos.feature.assignments.di

import com.studentos.feature.assignments.data.repository.AssignmentRepositoryImpl
import com.studentos.feature.assignments.data.scheduler.AssignmentReminderSchedulerImpl
import com.studentos.feature.assignments.domain.repository.AssignmentRepository
import com.studentos.feature.assignments.domain.scheduler.AssignmentReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AssignmentsModule {

    @Binds
    @Singleton
    abstract fun bindAssignmentRepository(
        impl: AssignmentRepositoryImpl
    ): AssignmentRepository

    @Binds
    @Singleton
    abstract fun bindAssignmentReminderScheduler(
        impl: AssignmentReminderSchedulerImpl
    ): AssignmentReminderScheduler
}
