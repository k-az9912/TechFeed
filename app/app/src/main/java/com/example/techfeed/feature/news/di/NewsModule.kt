package com.example.techfeed.feature.news.di

import com.example.techfeed.feature.news.data.DefaultNewsRepository
import com.example.techfeed.feature.news.data.NewsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NewsModule {
    @Binds abstract fun bindNewsRepository(repository: DefaultNewsRepository): NewsRepository
}
