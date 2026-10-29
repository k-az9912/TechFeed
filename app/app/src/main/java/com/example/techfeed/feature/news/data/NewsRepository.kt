package com.example.techfeed.feature.news.data

import com.example.techfeed.core.database.ArticleDao
import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.model.Article
import com.example.techfeed.feature.news.data.remote.FeedClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository
@Inject
constructor(
    private val dao: ArticleDao,
    private val feedClient: FeedClient,
) {
    val saved = dao.observe()

    suspend fun toggle(article: Article) = dao.toggle(article)

    suspend fun fetch(source: FeedSource): List<Article> = feedClient.fetch(source)
}
