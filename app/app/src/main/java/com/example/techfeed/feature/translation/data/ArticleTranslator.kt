package com.example.techfeed.feature.translation.data

import com.example.techfeed.core.model.Article
import com.example.techfeed.feature.translation.model.ArticleTranslation
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translator
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

@ViewModelScoped
class ArticleTranslator @Inject constructor(private val client: Translator) : AutoCloseable {

    suspend fun isModelReady(): Boolean =
        RemoteModelManager.getInstance()
            .isModelDownloaded(TranslateRemoteModel.Builder(TranslateLanguage.JAPANESE).build())
            .await()

    suspend fun downloadModel() {
        client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
    }

    suspend fun translate(article: Article): ArticleTranslation {
        val title = client.translate(article.title).await()
        val summary =
            if (article.summary.isBlank()) "" else client.translate(article.summary).await()
        return ArticleTranslation(title = title, summary = summary)
    }

    override fun close() = client.close()
}
