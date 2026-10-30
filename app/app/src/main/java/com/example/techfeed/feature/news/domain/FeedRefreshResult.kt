package com.example.techfeed.feature.news.domain

import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.model.Article

internal data class FeedRefreshResult(
    val articles: List<Article>,
    val failedSources: List<String>,
)

internal fun resolveFeedResults(
    previous: List<Article>,
    results: List<Pair<FeedSource, Result<List<Article>>>>,
): FeedRefreshResult {
    val allFailed = results.isNotEmpty() && results.all { it.second.isFailure }
    val articles =
        if (allFailed) previous
        else
            orderArticles(
                results.flatMap { (_, result) -> result.getOrDefault(emptyList()) },
            )
    return FeedRefreshResult(
        articles = articles,
        failedSources = results.filter { it.second.isFailure }.map { it.first.name },
    )
}
