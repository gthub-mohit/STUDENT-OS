package com.studentos.core.notifications.di

import com.studentos.core.notifications.dispatcher.NotificationDispatcher
import com.studentos.core.notifications.dispatcher.NotificationDispatcherImpl
import com.studentos.core.notifications.scheduler.NotificationRescheduler
import com.studentos.core.notifications.scheduler.NotificationReschedulerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    @Singleton
    abstract fun bindNotificationDispatcher(
        impl: NotificationDispatcherImpl
    ): NotificationDispatcher

    @Binds
    @Singleton
    abstract fun bindNotificationRescheduler(
        impl: NotificationReschedulerImpl
    ): NotificationRescheduler
}
