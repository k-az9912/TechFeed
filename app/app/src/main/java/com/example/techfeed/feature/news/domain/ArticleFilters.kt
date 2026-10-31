package com.example.techfeed.feature.news.domain

import com.example.techfeed.core.model.Article
import com.example.techfeed.core.tags.TagDefinition
import com.example.techfeed.core.tags.articleTags
import com.example.techfeed.core.tags.defaultTagDefinitions

fun filterArticles(
    articles: List<Article>,
    source: String,
    tag: String,
    tags: List<TagDefinition> = defaultTagDefinitions,
): List<Article> =
    articles.filter {
        (source == "すべて" || it.source == source) &&
            (tag == "すべて" || tag in articleTags(it, tags))
    }
