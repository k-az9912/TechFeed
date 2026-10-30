package com.example.techfeed.core.di

import android.content.Context
import androidx.room.Room
import com.example.techfeed.core.database.ArticleDao
import com.example.techfeed.core.database.NewsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NewsDatabase =
        Room.databaseBuilder(context, NewsDatabase::class.java, "news.db").build()

    @Provides fun provideArticleDao(database: NewsDatabase): ArticleDao = database.articles()
}
