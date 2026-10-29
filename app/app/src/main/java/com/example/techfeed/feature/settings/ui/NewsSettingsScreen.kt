package com.example.techfeed.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.techfeed.R
import com.example.techfeed.core.feed.availableSources
import com.example.techfeed.core.tags.availableTags
import com.example.techfeed.core.ui.openExternalLink
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.translation.model.TRANSLATION_DISCLAIMER
import com.example.techfeed.feature.translation.model.TranslationModelState
import com.example.techfeed.feature.translation.ui.ModelDownloadDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsSettingsScreen(
    settings: NewsSettings?,
    error: String?,
    setSource: (String, Boolean) -> Unit,
    setTag: (String, Boolean) -> Unit,
    moveSource: (String, Int) -> Unit,
    moveTag: (String, Int) -> Unit,
    modelState: TranslationModelState,
    downloadModel: () -> Unit,
    back: () -> Unit,
) {
    val context = LocalContext.current
    var confirmDownload by rememberSaveable { mutableStateOf(false) }
    val sourceOrder = settings?.sourceOrder ?: availableSources
    val tagOrder = settings?.tagOrder ?: availableTags
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "戻る")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).testTag("settings_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (settings == null) Text("設定を読み込み中…")
                Text("取得するサイト・表示順", style = MaterialTheme.typography.titleLarge)
                Text("上下ボタンで取得元の表示順を変更できます。変更は自動保存します。取得を止めても保存済み記事は残ります。")
            }
            itemsIndexed(sourceOrder, key = { _, source -> "source:$source" }) { index, source ->
                OrderedSetting(
                    source,
                    settings != null,
                    settings?.sources?.contains(source) == true,
                    index,
                    sourceOrder.size,
                    { setSource(source, it) },
                    { moveSource(source, it) },
                )
            }
            item {
                Text("話題タグ・表示順", style = MaterialTheme.typography.titleLarge)
                Text("上下ボタンで並べ替え、スイッチで表示を切り替えます。タグを非表示にしても記事自体は消えません。")
            }
            itemsIndexed(tagOrder, key = { _, tag -> "tag:$tag" }) { index, tag ->
                OrderedSetting(
                    tag,
                    settings != null,
                    settings?.visibleTags?.contains(tag) == true,
                    index,
                    tagOrder.size,
                    { setTag(tag, it) },
                    { moveTag(tag, it) },
                )
            }
            item {
                Text("翻訳モデル", style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        modelState.downloading -> "ダウンロード中…"
                        modelState.ready == true -> "英語→日本語：取得済み"
                        modelState.ready == false -> "英語→日本語：未取得"
                        else -> "取得状態を確認中…"
                    },
                )
                TextButton(
                    onClick = { confirmDownload = true },
                    enabled = modelState.ready == false && !modelState.downloading,
                ) {
                    Text("翻訳モデルをダウンロード")
                }
                modelState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(
                    "Google Translate の技術を使用した ML Kit の端末内翻訳です。英語記事のタイトルと概要を日本語に翻訳します。翻訳は不正確な場合があるため、原文も確認してください。",
                )
                TextButton(
                    onClick = {
                        openExternalLink(context, "https://cloud.google.com/translate")
                    },
                ) {
                    Text("Google Translate の翻訳について")
                }
                Text(TRANSLATION_DISCLAIMER, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    if (confirmDownload) {
        ModelDownloadDialog(
            confirm = {
                confirmDownload = false
                downloadModel()
            },
            dismiss = { confirmDownload = false },
        )
    }
}

@Composable
private fun OrderedSetting(
    label: String,
    enabled: Boolean,
    checked: Boolean,
    index: Int,
    size: Int,
    change: (Boolean) -> Unit,
    move: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = { move(-1) }, enabled = enabled && index > 0) {
            Icon(painterResource(R.drawable.ic_up), contentDescription = "$label を前へ")
        }
        IconButton(onClick = { move(1) }, enabled = enabled && index < size - 1) {
            Icon(painterResource(R.drawable.ic_down), contentDescription = "$label を後ろへ")
        }
        Switch(
            modifier = Modifier.testTag("setting_switch:$label"),
            checked = checked,
            onCheckedChange = change,
            enabled = enabled,
        )
    }
}
