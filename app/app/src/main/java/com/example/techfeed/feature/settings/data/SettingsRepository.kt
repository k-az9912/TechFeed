package com.example.techfeed.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.techfeed.core.feed.availableSources
import com.example.techfeed.core.feed.feedSources
import com.example.techfeed.core.tags.availableTags
import com.example.techfeed.feature.settings.model.NewsSettings
import com.example.techfeed.feature.settings.model.moveInOrder
import com.example.techfeed.feature.settings.model.normalizeOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(private val store: DataStore<Preferences>) {
    private val disabledSources = stringSetPreferencesKey("disabled_sources")
    private val hiddenTags = stringSetPreferencesKey("hidden_tags")
    private val tagOrderKey = stringPreferencesKey("tag_order")
    private val sourceOrderKey = stringPreferencesKey("source_order")
    val settings =
        store.data.map { prefs ->
            NewsSettings(
                feedSources.map { it.name }.toSet() - (prefs[disabledSources] ?: emptySet()),
                availableTags.toSet() - (prefs[hiddenTags] ?: emptySet()),
                normalizeOrder(prefs[tagOrderKey]?.split("\n") ?: emptyList(), availableTags),
                normalizeOrder(prefs[sourceOrderKey]?.split("\n") ?: emptyList(), availableSources),
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

    suspend fun moveTag(tag: String, offset: Int) = move(tagOrderKey, availableTags, tag, offset)

    suspend fun moveSource(source: String, offset: Int) =
        move(sourceOrderKey, availableSources, source, offset)

    private suspend fun move(
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        available: List<String>,
        item: String,
        offset: Int,
    ) {
        store.edit { prefs ->
            val order = normalizeOrder(prefs[key]?.split("\n") ?: emptyList(), available)
            prefs[key] = moveInOrder(order, item, offset).joinToString("\n")
        }
    }
}
