package com.example.techfeed.feature.news.domain

import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.model.Article
import org.junit.Assert.*
import org.junit.Test

class FeedRefreshResultTest {
    private val first = FeedSource("First", "開発", "https://example.com/feed")
    private val second = first.copy(name = "Second")
    private val old = Article("old", first.name, "開発", "Old", "", "2026-10-27")
    private val fresh = old.copy(id = "new", title = "New")
    private val failure = Result.failure<List<Article>>(IllegalStateException("HTTP 503"))

    @Test fun successfulEmptyFeedClearsPreviousArticles() {
        val result = resolveFeedResults(listOf(old), listOf(first to Result.success(emptyList())))
        assertTrue(result.articles.isEmpty())
        assertTrue(result.failedSources.isEmpty())
    }

    @Test fun allFailuresKeepPreviousArticlesAndReportSources() {
        val result = resolveFeedResults(listOf(old), listOf(first to failure, second to failure))
        assertEquals(listOf(old), result.articles)
        assertEquals(listOf(first.name, second.name), result.failedSources)
    }

    @Test fun partialSuccessUsesOnlyNewArticles() {
        val result = resolveFeedResults(listOf(old), listOf(first to failure, second to Result.success(listOf(fresh))))
        assertEquals(listOf(fresh), result.articles)
        assertEquals(listOf(first.name), result.failedSources)
    }

    @Test fun emptySuccessWithAnotherFailureStillClearsOldArticles() {
        val result = resolveFeedResults(listOf(old), listOf(first to Result.success(emptyList()), second to failure))
        assertTrue(result.articles.isEmpty())
        assertEquals(listOf(second.name), result.failedSources)
    }

    @Test fun noEnabledSourcesClearsArticlesWithoutError() {
        val result = resolveFeedResults(listOf(old), emptyList())
        assertTrue(result.articles.isEmpty())
        assertTrue(result.failedSources.isEmpty())
    }

    @Test fun successOrdersAndDeduplicatesArticles() {
        val newer = fresh.copy(id = "newer", time = "2026-10-28T10:00:00Z")
        val older = fresh.copy(time = "2026-10-27T10:00:00Z")
        val result = resolveFeedResults(emptyList(), listOf(
            first to Result.success(listOf(older, newer)),
            second to Result.success(listOf(newer))
        ))
        assertEquals(listOf(newer, older), result.articles)
    }
}
