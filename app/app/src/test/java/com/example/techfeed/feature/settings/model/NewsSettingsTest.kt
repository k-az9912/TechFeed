package com.example.techfeed.feature.settings.model

import com.example.techfeed.core.feed.feedSources
import com.example.techfeed.core.tags.availableTags
import org.junit.Assert.*
import org.junit.Test

class NewsSettingsTest {

    @Test fun defaultsIncludeAllSourcesAndTags() {
        val settings = NewsSettings()
        assertEquals(feedSources.map { it.name }.toSet(), settings.sources)
        assertEquals(availableTags.toSet(), settings.visibleTags)
    }
}
