package com.example.techfeed.feature.notifications

import com.example.techfeed.feature.news.data.NewsRepository
import com.example.techfeed.feature.settings.data.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationWorkerEntryPoint {
    fun newsRepository(): NewsRepository

    fun settingsRepository(): SettingsRepository
}
