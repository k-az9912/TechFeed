package com.example.techfeed.feature.news.domain

import com.example.techfeed.core.model.Article
import org.junit.Assert.*
import org.junit.Test

class ArticleOrderingTest {

    @Test fun zennTrendOrderIsPreserved() {
        val articles = listOf(
            Article("first", "Zenn", "Zennトレンド", "First", "", "2026-01-01T00:00:00Z"),
            Article("second", "Zenn", "Zennトレンド", "Second", "", "2026-10-28T00:00:00Z")
        )
        assertEquals(listOf("first", "second"), orderArticles(articles).map { it.id })
    }

    @Test fun trendOrderIsPreservedWhileNewsIsSortedByDate() {
        fun article(id: String, source: String, time: String) =
            Article(id, source, "開発", id, "", time)
        val ordered = orderArticles(listOf(
            article("trend1", "Qiita", "2026-10-01T00:00:00Z"),
            article("old", "News", "2026-10-20T00:00:00Z"),
            article("trend2", "Qiita", "2026-10-28T00:00:00Z"),
            article("new", "News", "2026-10-28T00:00:00Z")
        ))
        assertEquals(listOf("new", "old", "trend1", "trend2"), ordered.map { it.id })
    }

    @Test fun bothDateFormatsSortChronologically() {
        assertTrue(publicationTime("2026-10-28T09:00:00Z") >
            publicationTime("Tue, 27 Oct 2026 18:00:00 +0900"))
    }
}
