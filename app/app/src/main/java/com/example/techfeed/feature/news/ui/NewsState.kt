package com.example.techfeed.feature.news.ui

import com.example.techfeed.core.model.Article

data class NewsState(
    val articles: List<Article> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)
