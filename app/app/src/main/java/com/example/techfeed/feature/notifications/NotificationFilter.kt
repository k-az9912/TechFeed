package com.example.techfeed.feature.notifications

import com.example.techfeed.core.model.Article
import com.example.techfeed.core.tags.TagDefinition
import com.example.techfeed.core.tags.articleTags

fun Article.matchesNotificationFilter(
    sources: Set<String>?,
    tags: Set<String>?,
    tagDefinitions: List<TagDefinition>,
): Boolean {
    val sourceMatches = sources == null || source in sources
    val tagMatches = tags == null || articleTags(this, tagDefinitions).any { it in tags }
    return sourceMatches && tagMatches
}
