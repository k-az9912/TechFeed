package com.example.techfeed.feature.translation.model

import com.example.techfeed.core.ui.UiText

data class ArticleTranslation(
    val loading: Boolean = false,
    val title: String? = null,
    val summary: String? = null,
    val error: UiText? = null,
)

data class TranslationModelState(
    val ready: Boolean? = null,
    val downloading: Boolean = false,
    val error: UiText? = null,
)
