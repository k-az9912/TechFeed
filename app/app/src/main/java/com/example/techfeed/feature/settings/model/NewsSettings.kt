package com.example.techfeed.feature.settings.model

import com.example.techfeed.core.feed.FeedSource
import com.example.techfeed.core.feed.availableSources
import com.example.techfeed.core.feed.feedSources
import com.example.techfeed.core.tags.TagDefinition
import com.example.techfeed.core.tags.availableTags
import com.example.techfeed.core.tags.defaultTagDefinitions

data class NewsSettings(
    val sources: Set<String> = feedSources.map { it.name }.toSet(),
    val visibleTags: Set<String> = availableTags.toSet(),
    val tagOrder: List<String> = availableTags,
    val sourceOrder: List<String> = availableSources,
    val feeds: List<FeedSource> = feedSources,
    val tags: List<TagDefinition> = defaultTagDefinitions,
)
