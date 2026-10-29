package com.example.techfeed.feature.news.ui

import com.example.techfeed.core.model.Article
import com.example.techfeed.core.ui.UiText

data class NewsState(
    val articles: List<Article> = emptyList(),
    val loading: Boolean = false,
    val error: UiText? = null,
)
