package com.example.techfeed.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.techfeed.core.model.Article

@Database(entities = [Article::class], version = 1, exportSchema = true)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun articles(): ArticleDao
}
