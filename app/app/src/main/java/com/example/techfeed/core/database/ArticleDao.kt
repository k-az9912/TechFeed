package com.example.techfeed.core.database

import androidx.room.*
import com.example.techfeed.core.model.Article
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ArticleDao {
    @Query("SELECT * FROM saved_articles ORDER BY rowid DESC")
    abstract fun observe(): Flow<List<Article>>

    @Upsert abstract suspend fun save(article: Article)

    @Query("DELETE FROM saved_articles WHERE id = :id") abstract suspend fun remove(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_articles WHERE id = :id)")
    abstract suspend fun contains(id: String): Boolean

    @Transaction
    open suspend fun toggle(article: Article) {
        if (contains(article.id)) remove(article.id) else save(article)
    }
}
