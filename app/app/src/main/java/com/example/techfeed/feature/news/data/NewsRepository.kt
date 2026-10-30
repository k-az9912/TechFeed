package com.example.techfeed.feature.news.data

import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.model.Article
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    val saved: Flow<List<Article>>

    suspend fun toggle(article: Article)

    suspend fun fetch(source: FeedSource): List<Article>
}
