package com.example.techfeed.core.feed

import com.example.techfeed.core.model.Article

data class FeedSource(
    val name: String,
    val category: String,
    val url: String,
    val limit: Int = 100,
    val language: String = "ja",
)

val feedSources =
    listOf(
        FeedSource(
            "Android Developers",
            "Android",
            "https://developer.android.com/static/blog/atom.xml",
            language = "en",
        ),
        FeedSource("Qiita", "Qiitaトレンド", "https://qiita.com/popular-items/feed.atom", limit = 30),
        FeedSource("Zenn", "Zennトレンド", "https://zenn.dev/feed", limit = 30),
        FeedSource("IPA お知らせ", "IPA", "https://www.ipa.go.jp/about/newsonly-rss.rdf"),
        FeedSource("IPA セキュリティ", "セキュリティ", "https://www.ipa.go.jp/security/alert-rss.rdf"),
    )

val availableSources = feedSources.map { it.name }

fun isEnglishArticle(article: Article): Boolean = feedSources.any {
    it.name == article.source && it.language == "en"
}
