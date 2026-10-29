package com.example.techfeed.feature.news.domain

import com.example.techfeed.core.model.Article
import com.example.techfeed.core.tags.articleTags

fun filterArticles(articles: List<Article>, source: String, tag: String): List<Article> =
    articles.filter {
        (source == "すべて" || it.source == source) && (tag == "すべて" || tag in articleTags(it))
    }
