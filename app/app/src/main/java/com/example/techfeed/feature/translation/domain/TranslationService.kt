package com.example.techfeed.feature.translation.domain

import com.example.techfeed.core.model.Article
import com.example.techfeed.feature.translation.model.ArticleTranslation

interface TranslationService : AutoCloseable {
    suspend fun isModelReady(): Boolean

    suspend fun downloadModel()

    suspend fun translate(article: Article): ArticleTranslation
}
