package com.example.techfeed.feature.settings.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlin.math.roundToInt

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
    setNotificationsEnabled: (Boolean) -> Unit = {},
    setNotificationIntervalMinutes: (Int) -> Unit = {},
    setNotificationSource: (String?) -> Unit = {},
    setNotificationTag: (String?) -> Unit = {},
) {
    val context = LocalContext.current
    var confirmDownload by rememberSaveable { mutableStateOf(false) }
    var feedUrl by rememberSaveable { mutableStateOf("") }
    var tagName by rememberSaveable { mutableStateOf("") }
    var tagKeywords by rememberSaveable { mutableStateOf("") }
    var pendingFeedRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingTagRemoval by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationPermissionDenied by rememberSaveable { mutableStateOf(false) }
    var showNotificationSources by rememberSaveable { mutableStateOf(false) }
    var showNotificationTags by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationPermissionDenied = !granted
            if (granted) setNotificationsEnabled(true)
        }
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
                    stringResource(R.string.settings_notifications_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(stringResource(R.string.settings_notifications_hint))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_notifications_enabled),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = settings?.notificationsEnabled == true,
                        enabled = settings != null,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                setNotificationsEnabled(false)
                            } else if (
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                    context.checkSelfPermission(
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                setNotificationsEnabled(true)
                            } else {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            }
                        },
                        modifier = Modifier.testTag("notification_switch"),
                    )
                }
                if (notificationPermissionDenied) {
                    Text(
                        stringResource(R.string.notification_permission_denied),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(stringResource(R.string.settings_notification_interval))
                val intervals = listOf(15, 30, 60, 180, 360, 720, 1_440)
                var intervalIndex by remember(settings?.notificationIntervalMinutes) {
                    mutableFloatStateOf(
                        intervals.indexOf(settings?.notificationIntervalMinutes)
                            .coerceAtLeast(0)
                            .toFloat(),
                    )
                }
                val selectedMinutes = intervals[intervalIndex.roundToInt()]
                Text(
                    if (selectedMinutes < 60) {
                        stringResource(R.string.notification_minutes, selectedMinutes)
                    } else {
                        stringResource(R.string.notification_hours, selectedMinutes / 60)
                    },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = intervalIndex,
                    onValueChange = { value ->
                        val newIndex = value.roundToInt().coerceIn(intervals.indices)
                        if (newIndex != intervalIndex.roundToInt()) {
                            intervalIndex = newIndex.toFloat()
                            setNotificationIntervalMinutes(intervals[newIndex])
                        } else {
                            intervalIndex = value
                        }
                    },
                    enabled = settings != null,
                    valueRange = 0f..intervals.lastIndex.toFloat(),
                    steps = intervals.size - 2,
                    modifier = Modifier.fillMaxWidth().testTag("notification_interval_slider"),
                )
                NotificationFilterSummaryRow(
                    title = stringResource(R.string.settings_notification_sources),
                    selected = settings?.notificationSources,
                    enabled = settings != null,
                    modifier = Modifier.testTag("notification_sources_row"),
                    open = { showNotificationSources = true },
                )
                HorizontalDivider()
                NotificationFilterSummaryRow(
                    title = stringResource(R.string.settings_notification_tags),
                    selected = settings?.notificationTags,
                    enabled = settings != null,
                    modifier = Modifier.testTag("notification_tags_row"),
                    open = { showNotificationTags = true },
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
    if (showNotificationSources) {
        NotificationFilterSheet(
            title = stringResource(R.string.settings_notification_sources),
            values = sourceOrder,
            selected = settings?.notificationSources,
            tagPrefix = "notification_source",
            enabled = settings != null,
            selectAll = { setNotificationSource(null) },
            toggle = setNotificationSource,
            dismiss = { showNotificationSources = false },
        )
    }
    if (showNotificationTags) {
        NotificationFilterSheet(
            title = stringResource(R.string.settings_notification_tags),
            values = tagOrder,
            selected = settings?.notificationTags,
            tagPrefix = "notification_tag",
            enabled = settings != null,
            selectAll = { setNotificationTag(null) },
            toggle = setNotificationTag,
            dismiss = { showNotificationTags = false },
        )
    }
}

@Composable
private fun NotificationFilterSummaryRow(
    title: String,
    selected: Set<String>?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    open: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                if (selected == null) stringResource(R.string.all)
                else stringResource(R.string.notification_selected_count, selected.size),
            )
        },
        trailingContent = { Text("›", style = MaterialTheme.typography.headlineSmall) },
        modifier = modifier.fillMaxWidth().clickable(enabled = enabled, onClick = open),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationFilterSheet(
    title: String,
    values: List<String>,
    selected: Set<String>?,
    tagPrefix: String,
    enabled: Boolean,
    selectAll: () -> Unit,
    toggle: (String) -> Unit,
    dismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = dismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = dismiss) { Text(stringResource(R.string.close)) }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            item {
                NotificationFilterOption(
                    label = stringResource(R.string.all),
                    checked = selected == null,
                    enabled = enabled,
                    modifier = Modifier.testTag("$tagPrefix:all"),
                    select = selectAll,
                )
            }
            items(values, key = { it }) { value ->
                NotificationFilterOption(
                    label = value,
                    checked = value in selected.orEmpty(),
                    enabled = enabled,
                    modifier = Modifier.testTag("$tagPrefix:$value"),
                    select = { toggle(value) },
                )
            }
        }
    }
}

@Composable
private fun NotificationFilterOption(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    select: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(enabled = enabled, onClick = select).padding(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
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
