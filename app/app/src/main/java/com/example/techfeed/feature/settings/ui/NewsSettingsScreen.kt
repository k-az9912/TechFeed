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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.techfeed.R
import com.example.techfeed.core.feed.availableSources
import com.example.techfeed.core.tags.availableTags
import com.example.techfeed.core.ui.UiText
import com.example.techfeed.core.ui.openExternalLink
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.translation.model.TranslationModelState
import com.example.techfeed.feature.translation.ui.ModelDownloadDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsSettingsScreen(
    settings: NewsSettings?,
    error: UiText?,
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
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(
                            painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                        )
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
                error?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
                if (settings == null) Text(stringResource(R.string.settings_loading))
                Text(
                    stringResource(R.string.settings_sources_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.settings_sources_hint))
            }
            itemsIndexed(sourceOrder, key = { _, source -> "source:$source" }) { index, source ->
                OrderedSetting(
                    label = source,
                    enabled = settings != null,
                    checked = settings?.sources?.contains(source) == true,
                    index = index,
                    size = sourceOrder.size,
                    change = { setSource(source, it) },
                    move = { moveSource(source, it) },
                )
            }
            item {
                Text(
                    stringResource(R.string.settings_tags_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.settings_tags_hint))
            }
            itemsIndexed(tagOrder, key = { _, tag -> "tag:$tag" }) { index, tag ->
                OrderedSetting(
                    label = tag,
                    enabled = settings != null,
                    checked = settings?.visibleTags?.contains(tag) == true,
                    index = index,
                    size = tagOrder.size,
                    change = { setTag(tag, it) },
                    move = { moveTag(tag, it) },
                )
            }
            item {
                Text(
                    stringResource(R.string.translation_model),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    when {
                        modelState.downloading -> stringResource(R.string.downloading)
                        modelState.ready == true -> stringResource(R.string.model_ready)
                        modelState.ready == false -> stringResource(R.string.model_missing)
                        else -> stringResource(R.string.model_checking)
                    },
                )
                TextButton(
                    onClick = { confirmDownload = true },
                    enabled = modelState.ready == false && !modelState.downloading,
                ) {
                    Text(stringResource(R.string.download_model))
                }
                modelState.error?.let {
                    Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                }
                Text(stringResource(R.string.translation_explanation))
                TextButton(
                    onClick = {
                        openExternalLink(context, "https://cloud.google.com/translate")
                    },
                ) {
                    Text(stringResource(R.string.translation_about))
                }
                Text(
                    stringResource(R.string.translation_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                )
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
            Icon(
                painterResource(R.drawable.ic_up),
                contentDescription = stringResource(R.string.move_before, label),
            )
        }
        IconButton(onClick = { move(1) }, enabled = enabled && index < size - 1) {
            Icon(
                painterResource(R.drawable.ic_down),
                contentDescription = stringResource(R.string.move_after, label),
            )
        }
        Switch(
            modifier = Modifier.testTag("setting_switch:$label"),
            checked = checked,
            onCheckedChange = change,
            enabled = enabled,
        )
    }
}
