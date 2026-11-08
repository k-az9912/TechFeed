package com.example.techfeed.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.feed.feedSources
import com.example.techfeed.core.tags.TagDefinition
import com.example.techfeed.core.tags.defaultTagDefinitions
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.settings.model.moveInOrder
import com.example.techfeed.feature.settings.model.normalizeOrder
import com.example.techfeed.feature.notifications.data.calculateNotificationHistory
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    private val disabledSources = stringSetPreferencesKey("disabled_sources")
    private val hiddenTags = stringSetPreferencesKey("hidden_tags")
    private val tagOrderKey = stringPreferencesKey("tag_order")
    private val sourceOrderKey = stringPreferencesKey("source_order")
    private val customSourcesKey = stringPreferencesKey("custom_sources")
    private val customTagsKey = stringPreferencesKey("custom_tags")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val notificationIntervalMinutesKey = intPreferencesKey("notification_interval_minutes")
    private val legacyNotificationIntervalHoursKey = intPreferencesKey("notification_interval_hours")
    private val notificationSourcesKey = stringSetPreferencesKey("notification_sources")
    private val notificationTagsKey = stringSetPreferencesKey("notification_tags")
    private val notificationHistoryInitializedKey =
        booleanPreferencesKey("notification_history_initialized")
    private val notifiedArticleHashesKey = stringSetPreferencesKey("notified_article_hashes")
    val settings =
        store.data.map { prefs ->
            val customSources = decodeSources(prefs[customSourcesKey])
            val customTags = decodeTags(prefs[customTagsKey])
            val feeds = feedSources + customSources
            val tags = defaultTagDefinitions + customTags
            val sourceNames = feeds.map { it.name }
            val tagNames = tags.map { it.name }
            NewsSettings(
                sources = sourceNames.toSet() - (prefs[disabledSources] ?: emptySet()),
                visibleTags = tagNames.toSet() - (prefs[hiddenTags] ?: emptySet()),
                tagOrder =
                    normalizeOrder(prefs[tagOrderKey]?.split("\n") ?: emptyList(), tagNames),
                sourceOrder =
                    normalizeOrder(prefs[sourceOrderKey]?.split("\n") ?: emptyList(), sourceNames),
                feeds = feeds,
                tags = tags,
                notificationsEnabled = prefs[notificationsEnabledKey] ?: false,
                notificationIntervalMinutes =
                    (prefs[notificationIntervalMinutesKey]
                            ?: prefs[legacyNotificationIntervalHoursKey]?.times(60)
                            ?: DEFAULT_NOTIFICATION_INTERVAL_MINUTES)
                        .takeIf { it in NOTIFICATION_INTERVALS }
                        ?: DEFAULT_NOTIFICATION_INTERVAL_MINUTES,
                notificationSources =
                    prefs[notificationSourcesKey]
                        ?.intersect(sourceNames.toSet())
                        ?.takeIf { it.isNotEmpty() },
                notificationTags =
                    prefs[notificationTagsKey]
                        ?.intersect(tagNames.toSet())
                        ?.takeIf { it.isNotEmpty() },
            )
        }

    suspend fun setSource(name: String, enabled: Boolean) {
        store.edit { prefs ->
            val old = prefs[disabledSources] ?: emptySet()
            prefs[disabledSources] = if (enabled) old - name else old + name
            resetNotificationHistory(prefs)
        }
    }

    suspend fun setTag(tag: String, visible: Boolean) {
        store.edit { prefs ->
            val old = prefs[hiddenTags] ?: emptySet()
            prefs[hiddenTags] = if (visible) old - tag else old + tag
        }
    }

    suspend fun moveTag(tag: String, offset: Int) =
        store.edit { prefs ->
            val available =
                defaultTagDefinitions.map { it.name } +
                    decodeTags(prefs[customTagsKey]).map { it.name }
            val order = normalizeOrder(prefs[tagOrderKey]?.split("\n") ?: emptyList(), available)
            prefs[tagOrderKey] = moveInOrder(order, tag, offset).joinToString("\n")
        }

    suspend fun moveSource(source: String, offset: Int) =
        store.edit { prefs ->
            val available =
                feedSources.map { it.name } +
                    decodeSources(prefs[customSourcesKey]).map { it.name }
            val order = normalizeOrder(prefs[sourceOrderKey]?.split("\n") ?: emptyList(), available)
            prefs[sourceOrderKey] = moveInOrder(order, source, offset).joinToString("\n")
        }

    suspend fun addCustomSource(source: FeedSource) {
        store.edit { prefs ->
            val custom = decodeSources(prefs[customSourcesKey])
            require((feedSources + custom).none { it.url == source.url }) { "Feed already exists" }
            val existingNames = (feedSources + custom).map { it.name }.toSet()
            val uniqueSource = source.copy(name = uniqueName(source.name, existingNames))
            prefs[customSourcesKey] = encodeSources(custom + uniqueSource)
            prefs[disabledSources] = (prefs[disabledSources] ?: emptySet()) - uniqueSource.name
            val available = (feedSources + custom + uniqueSource).map { it.name }
            prefs[sourceOrderKey] =
                normalizeOrder(prefs[sourceOrderKey]?.split("\n") ?: emptyList(), available)
                    .joinToString("\n")
            resetNotificationHistory(prefs)
        }
    }

    suspend fun removeCustomSource(name: String) {
        store.edit { prefs ->
            val remaining = decodeSources(prefs[customSourcesKey]).filterNot { it.name == name }
            prefs[customSourcesKey] = encodeSources(remaining)
            prefs[disabledSources] = (prefs[disabledSources] ?: emptySet()) - name
            val available = (feedSources + remaining).map { it.name }
            prefs[sourceOrderKey] =
                normalizeOrder(prefs[sourceOrderKey]?.split("\n") ?: emptyList(), available)
                    .joinToString("\n")
            val notificationSources = prefs[notificationSourcesKey]
            if (notificationSources != null) {
                val remainingNotificationSources = notificationSources - name
                if (remainingNotificationSources.isEmpty()) prefs.remove(notificationSourcesKey)
                else prefs[notificationSourcesKey] = remainingNotificationSources
            }
            resetNotificationHistory(prefs)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        store.edit { prefs ->
            prefs[notificationsEnabledKey] = enabled
            if (!enabled) resetNotificationHistory(prefs)
        }
    }

    suspend fun setNotificationIntervalMinutes(minutes: Int) {
        require(minutes in NOTIFICATION_INTERVALS)
        store.edit { prefs ->
            prefs[notificationIntervalMinutesKey] = minutes
            prefs.remove(legacyNotificationIntervalHoursKey)
        }
    }

    suspend fun setNotificationTag(tag: String?) {
        store.edit { prefs ->
            if (tag == null) {
                prefs.remove(notificationTagsKey)
                return@edit
            }
            val available =
                (defaultTagDefinitions + decodeTags(prefs[customTagsKey])).map { it.name }.toSet()
            require(tag in available)
            val current = prefs[notificationTagsKey]
            val updated = if (current == null) setOf(tag) else current.toggle(tag)
            if (updated.isEmpty()) prefs.remove(notificationTagsKey)
            else prefs[notificationTagsKey] = updated
        }
    }

    suspend fun setNotificationSource(source: String?) {
        store.edit { prefs ->
            if (source == null) {
                prefs.remove(notificationSourcesKey)
                return@edit
            }
            val available =
                (feedSources + decodeSources(prefs[customSourcesKey])).map { it.name }.toSet()
            require(source in available)
            val current = prefs[notificationSourcesKey]
            val updated = if (current == null) setOf(source) else current.toggle(source)
            if (updated.isEmpty()) prefs.remove(notificationSourcesKey)
            else prefs[notificationSourcesKey] = updated
        }
    }

    suspend fun updateNotificationHistory(
        articleIds: List<String>,
        allowInitialization: Boolean,
    ): Set<String> {
        var newIds = emptySet<String>()
        store.edit { prefs ->
            val initialized = prefs[notificationHistoryInitializedKey] ?: false
            if (!initialized && !allowInitialization) return@edit
            val update =
                calculateNotificationHistory(
                    articleIds = articleIds,
                    previousHashes = prefs[notifiedArticleHashesKey] ?: emptySet(),
                    initialized = initialized,
                )
            newIds = update.newArticleIds.toSet()
            prefs[notifiedArticleHashesKey] = update.storedHashes
            prefs[notificationHistoryInitializedKey] = true
        }
        return newIds
    }

    private fun resetNotificationHistory(prefs: androidx.datastore.preferences.core.MutablePreferences) {
        prefs[notificationHistoryInitializedKey] = false
        prefs.remove(notifiedArticleHashesKey)
    }

    suspend fun addCustomTag(name: String, keywords: List<String>) {
        store.edit { prefs ->
            val custom = decodeTags(prefs[customTagsKey])
            val normalizedName = name.trim().take(40)
            val normalizedKeywords =
                keywords.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)
            require(normalizedName.isNotEmpty() && normalizedKeywords.isNotEmpty())
            require((defaultTagDefinitions + custom).none { it.name == normalizedName })
            val tag = TagDefinition(normalizedName, normalizedKeywords, custom = true)
            prefs[customTagsKey] = encodeTags(custom + tag)
            prefs[hiddenTags] = (prefs[hiddenTags] ?: emptySet()) - normalizedName
            val available = (defaultTagDefinitions + custom + tag).map { it.name }
            prefs[tagOrderKey] =
                normalizeOrder(prefs[tagOrderKey]?.split("\n") ?: emptyList(), available)
                    .joinToString("\n")
        }
    }

    suspend fun removeCustomTag(name: String) {
        store.edit { prefs ->
            val remaining = decodeTags(prefs[customTagsKey]).filterNot { it.name == name }
            prefs[customTagsKey] = encodeTags(remaining)
            prefs[hiddenTags] = (prefs[hiddenTags] ?: emptySet()) - name
            val available = (defaultTagDefinitions + remaining).map { it.name }
            prefs[tagOrderKey] =
                normalizeOrder(prefs[tagOrderKey]?.split("\n") ?: emptyList(), available)
                    .joinToString("\n")
            val notificationTags = prefs[notificationTagsKey]
            if (notificationTags != null) {
                val remainingNotificationTags = notificationTags - name
                if (remainingNotificationTags.isEmpty()) prefs.remove(notificationTagsKey)
                else prefs[notificationTagsKey] = remainingNotificationTags
            }
        }
    }

    private fun uniqueName(value: String, existing: Set<String>): String {
        val base = value.trim().ifBlank { "カスタムフィード" }.take(80)
        if (base !in existing) return base
        var suffix = 2
        while ("$base ($suffix)" in existing) suffix++
        return "$base ($suffix)"
    }

    private fun encodeSources(sources: List<FeedSource>): String =
        sources.joinToString("\n") { "${encode(it.name)}\t${encode(it.url)}" }

    private fun decodeSources(value: String?): List<FeedSource> =
        value
            ?.lineSequence()
            ?.mapNotNull { line ->
                val fields = line.split('\t')
                if (fields.size != 2) return@mapNotNull null
                runCatching {
                        FeedSource(
                            name = decode(fields[0]),
                            category = "ITニュース",
                            url = decode(fields[1]),
                        )
                    }
                    .getOrNull()
            }
            ?.filter { it.name.isNotBlank() && it.url.isNotBlank() }
            ?.distinctBy { it.url }
            ?.toList()
            ?: emptyList()

    private fun encodeTags(tags: List<TagDefinition>): String =
        tags.joinToString("\n") { tag ->
            "${encode(tag.name)}\t${tag.keywords.joinToString(",") { encode(it) }}"
        }

    private fun decodeTags(value: String?): List<TagDefinition> =
        value
            ?.lineSequence()
            ?.mapNotNull { line ->
                val fields = line.split('\t')
                if (fields.size != 2) return@mapNotNull null
                runCatching {
                        val keywords = fields[1].split(',').map(::decode).filter { it.isNotBlank() }
                        TagDefinition(decode(fields[0]), keywords, custom = true)
                    }
                    .getOrNull()
            }
            ?.filter { it.name.isNotBlank() && it.keywords.isNotEmpty() }
            ?.distinctBy { it.name }
            ?.toList()
            ?: emptyList()

    private fun encode(value: String): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    companion object {
        val NOTIFICATION_INTERVALS = setOf(15, 30, 60, 180, 360, 720, 1_440)
        const val DEFAULT_NOTIFICATION_INTERVAL_MINUTES = 720
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value
