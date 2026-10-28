package com.example.techfeed.feature.news.domain

import com.example.techfeed.core.model.Article
internal fun publicationTime(value: String): java.time.Instant =
    runCatching { java.time.OffsetDateTime.parse(value).toInstant() }.getOrElse {
        runCatching { java.time.ZonedDateTime.parse(value, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
            .getOrDefault(java.time.Instant.EPOCH)
    }

// ニュースは日時順、トレンドは各配信元のフィード順で末尾にまとめる。
internal fun orderArticles(articles: List<Article>): List<Article> {
    val unique = articles.distinctBy { it.id }
    val (trending, news) = unique.partition { it.source in setOf("Qiita", "Zenn") }
    return news.sortedByDescending { publicationTime(it.time) } + trending
}
