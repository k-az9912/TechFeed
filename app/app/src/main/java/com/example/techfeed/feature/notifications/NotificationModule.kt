package com.example.techfeed.feature.notifications

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds
    abstract fun bindNotificationScheduler(
        scheduler: WorkManagerNotificationScheduler,
    ): NotificationScheduler
}
