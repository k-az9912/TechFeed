package com.example.techfeed.feature.news.domain

import com.example.techfeed.core.model.Article
import com.example.techfeed.core.time.parseArticleTime

internal fun publicationTime(value: String): java.time.Instant =
    parseArticleTime(value) ?: java.time.Instant.EPOCH

// ニュースは日時順、トレンドは各配信元のフィード順で末尾にまとめる。
internal fun orderArticles(articles: List<Article>): List<Article> {
    val unique = articles.distinctBy { it.id }
    val (trending, news) = unique.partition { it.source in setOf("Qiita", "Zenn") }
    return news.sortedByDescending { publicationTime(it.time) } + trending
}
