package com.example.techfeed.feature.news.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.techfeed.R
import com.example.techfeed.core.feed.isEnglishArticle
import com.example.techfeed.core.model.Article
import com.example.techfeed.core.time.formatArticleTime
import com.example.techfeed.core.ui.openExternalLink
import com.example.techfeed.feature.translation.model.ArticleTranslation
import com.example.techfeed.feature.translation.model.TranslationModelState
import com.example.techfeed.feature.translation.ui.ModelDownloadDialog

@Composable
fun ArticleDetailDialog(
    article: Article,
    translation: ArticleTranslation,
    modelState: TranslationModelState,
    downloadModel: () -> Unit,
    translate: () -> Unit,
    dismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showTranslated by rememberSaveable(article.id) { mutableStateOf(false) }
    var linkError by remember(article.id) { mutableStateOf(false) }
    var confirmDownload by rememberSaveable(article.id) { mutableStateOf(false) }
    val translated = showTranslated && translation.title != null
    fun openLink(url: String) {
        if (!openExternalLink(context, url)) linkError = true
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (translated) translation.title.orEmpty() else article.title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(
                        R.string.article_metadata,
                        article.source,
                        formatArticleTime(article.time) ?: stringResource(R.string.date_unknown),
                    ),
                )
                Text(if (translated) translation.summary.orEmpty() else article.summary)
                if (isEnglishArticle(article)) {
                    TextButton(
                        onClick = {
                            when {
                                translation.title != null -> showTranslated = !showTranslated
                                modelState.ready == true -> {
                                    showTranslated = true
                                    translate()
                                }
                                else -> confirmDownload = true
                            }
                        },
                        enabled =
                            !translation.loading &&
                                !modelState.downloading &&
                                modelState.ready != null,
                    ) {
                        Text(
                            when {
                                modelState.downloading -> stringResource(R.string.model_downloading)
                                translation.loading -> stringResource(R.string.translating)
                                translated -> stringResource(R.string.show_original)
                                translation.title != null ->
                                    stringResource(R.string.show_translation)
                                else -> stringResource(R.string.translate_google)
                            },
                        )
                    }
                    modelState.error?.let {
                        Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                    }
                    translation.error?.let {
                        Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                    }
                    if (translated) {
                        Text(
                            stringResource(R.string.translation_notice),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Image(
                            painterResource(R.drawable.google_translate_attribution),
                            contentDescription = stringResource(R.string.translation_attribution),
                            modifier =
                                Modifier.clickable { openLink("https://translate.google.com/") },
                        )
                    }
                }
                if (linkError) Text(stringResource(R.string.link_unavailable))
            }
        },
        confirmButton = {
            TextButton(onClick = { openLink(article.url) }) {
                Text(stringResource(R.string.open_article))
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) } },
    )
    if (confirmDownload) {
        ModelDownloadDialog(
            confirm = {
                confirmDownload = false
                showTranslated = true
                downloadModel()
            },
            dismiss = { confirmDownload = false },
        )
    }
}
