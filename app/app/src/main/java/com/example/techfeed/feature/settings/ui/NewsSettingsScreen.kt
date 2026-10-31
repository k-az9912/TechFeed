package com.example.techfeed.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.techfeed.R
import com.example.techfeed.core.feed.availableSources
import com.example.techfeed.core.tags.availableTags
import com.example.techfeed.core.ui.UiText
import com.example.techfeed.core.ui.openExternalLink
import com.example.techfeed.feature.news.ui.CustomFeedState
import com.example.techfeed.feature.news.ui.CustomTagState
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
    customFeedState: CustomFeedState,
    addCustomFeed: (String) -> Unit,
    removeCustomFeed: (String) -> Unit,
    modelState: TranslationModelState,
    downloadModel: () -> Unit,
    back: () -> Unit,
    customTagState: CustomTagState = CustomTagState(),
    addCustomTag: (String, String) -> Unit = { _, _ -> },
    removeCustomTag: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var confirmDownload by rememberSaveable { mutableStateOf(false) }
    var feedUrl by rememberSaveable { mutableStateOf("") }
    var tagName by rememberSaveable { mutableStateOf("") }
    var tagKeywords by rememberSaveable { mutableStateOf("") }
    var pendingFeedRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingTagRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    val sourceOrder = settings?.sourceOrder ?: availableSources
    val tagOrder = settings?.tagOrder ?: availableTags
    val customNames = settings?.feeds?.map { it.name }?.toSet().orEmpty() - availableSources.toSet()
    val customTagNames = settings?.tags?.filter { it.custom }?.map { it.name }?.toSet().orEmpty()
    LaunchedEffect(customFeedState.addedSource) {
        if (customFeedState.addedSource != null) feedUrl = ""
    }
    LaunchedEffect(customTagState.addedTag) {
        if (customTagState.addedTag != null) {
            tagName = ""
            tagKeywords = ""
        }
    }
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
                    stringResource(R.string.settings_add_feed_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.settings_add_feed_hint))
                OutlinedTextField(
                    value = feedUrl,
                    onValueChange = { feedUrl = it },
                    modifier = Modifier.fillMaxWidth().testTag("custom_feed_url"),
                    enabled = !customFeedState.adding,
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_feed_url)) },
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                        ),
                )
                Button(
                    onClick = { addCustomFeed(feedUrl) },
                    enabled = feedUrl.isNotBlank() && !customFeedState.adding,
                    modifier = Modifier.testTag("add_custom_feed"),
                ) {
                    Text(
                        if (customFeedState.adding) stringResource(R.string.checking_feed)
                        else stringResource(R.string.add_feed),
                    )
                }
                customFeedState.error?.let {
                    Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                }
                customFeedState.addedSource?.let {
                    Text(stringResource(R.string.custom_feed_added, it))
                }
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
                    removable = source in customNames,
                    remove = { pendingFeedRemoval = source },
                )
            }
            item {
                Text(
                    stringResource(R.string.settings_tags_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.settings_tags_hint))
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    modifier = Modifier.fillMaxWidth().testTag("custom_tag_name"),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_tag_name)) },
                )
                OutlinedTextField(
                    value = tagKeywords,
                    onValueChange = { tagKeywords = it },
                    modifier = Modifier.fillMaxWidth().testTag("custom_tag_keywords"),
                    minLines = 2,
                    label = { Text(stringResource(R.string.settings_tag_keywords)) },
                    supportingText = { Text(stringResource(R.string.settings_tag_keywords_hint)) },
                )
                Button(
                    onClick = { addCustomTag(tagName, tagKeywords) },
                    enabled = tagName.isNotBlank() && tagKeywords.isNotBlank(),
                    modifier = Modifier.testTag("add_custom_tag"),
                ) {
                    Text(stringResource(R.string.add_tag))
                }
                customTagState.error?.let {
                    Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                }
                customTagState.addedTag?.let {
                    Text(stringResource(R.string.custom_tag_added, it))
                }
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
                    removable = tag in customTagNames,
                    remove = { pendingTagRemoval = tag },
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
    pendingFeedRemoval?.let { source ->
        AlertDialog(
            onDismissRequest = { pendingFeedRemoval = null },
            title = { Text(stringResource(R.string.remove_feed_title)) },
            text = { Text(stringResource(R.string.remove_feed_message, source)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingFeedRemoval = null
                        removeCustomFeed(source)
                    },
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFeedRemoval = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    pendingTagRemoval?.let { tag ->
        AlertDialog(
            onDismissRequest = { pendingTagRemoval = null },
            title = { Text(stringResource(R.string.remove_tag_title)) },
            text = { Text(stringResource(R.string.remove_tag_message, tag)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingTagRemoval = null
                        removeCustomTag(tag)
                    },
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTagRemoval = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
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
    removable: Boolean = false,
    remove: () -> Unit = {},
) {
    val largeFont = LocalDensity.current.fontScale > 1.3f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = largeFont || maxWidth < 360.dp
        Column {
            if (stacked) Text(label)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                if (!stacked) Text(label, modifier = Modifier.weight(1f))
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
                if (removable) {
                    TextButton(onClick = remove, enabled = enabled) {
                        Text(stringResource(R.string.remove))
                    }
                }
            }
        }
    }
}
