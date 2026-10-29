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
import androidx.compose.ui.unit.dp
import com.example.techfeed.R
import com.example.techfeed.core.feed.isEnglishArticle
import com.example.techfeed.core.model.Article
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
                Text("${article.source} · ${article.time}")
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
                                modelState.downloading -> "モデル取得中…"
                                translation.loading -> "翻訳中…"
                                translated -> "原文を表示"
                                translation.title != null -> "日本語訳を表示"
                                else -> "Google で翻訳"
                            },
                        )
                    }
                    modelState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    translation.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (translated) {
                        Text("機械翻訳です。内容は原文でも確認してください。", style = MaterialTheme.typography.bodySmall)
                        Image(
                            painterResource(R.drawable.google_translate_attribution),
                            contentDescription = "powered by Google Translate",
                            modifier =
                                Modifier.clickable { openLink("https://translate.google.com/") },
                        )
                    }
                }
                if (linkError) Text("リンクを開けるアプリがありません。")
            }
        },
        confirmButton = { TextButton(onClick = { openLink(article.url) }) { Text("元記事を開く") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("閉じる") } },
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
