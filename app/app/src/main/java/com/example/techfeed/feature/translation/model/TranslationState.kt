package com.example.techfeed.feature.translation.model

data class ArticleTranslation(
    val loading: Boolean = false,
    val title: String? = null,
    val summary: String? = null,
    val error: String? = null,
)

data class TranslationModelState(
    val ready: Boolean? = null,
    val downloading: Boolean = false,
    val error: String? = null,
)
