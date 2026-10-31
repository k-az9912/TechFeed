package com.example.techfeed.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.feed.feedSources
import com.example.techfeed.core.tags.TagDefinition
import com.example.techfeed.core.tags.defaultTagDefinitions
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.settings.model.moveInOrder
import com.example.techfeed.feature.settings.model.normalizeOrder
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
            )
        }

    suspend fun setSource(name: String, enabled: Boolean) {
        store.edit { prefs ->
            val old = prefs[disabledSources] ?: emptySet()
            prefs[disabledSources] = if (enabled) old - name else old + name
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
        }
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
}
