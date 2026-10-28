package com.example.techfeed.core.database

import com.example.techfeed.core.model.Article
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Article::class], version = 1, exportSchema = true)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun articles(): ArticleDao
}
