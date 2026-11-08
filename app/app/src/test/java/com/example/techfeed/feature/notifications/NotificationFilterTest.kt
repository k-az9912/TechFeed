package com.example.techfeed.feature.notifications

import com.example.techfeed.core.model.Article
import com.example.techfeed.core.tags.defaultTagDefinitions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationFilterTest {
    private val article =
        Article(
            id = "https://example.com/android-security",
            source = "Android Developers",
            category = "Android",
            title = "Android security update",
            summary = "A vulnerability was fixed.",
            time = "2026-11-05",
        )

    @Test
    fun allFiltersAcceptArticle() {
        assertTrue(article.matchesNotificationFilter(null, null, defaultTagDefinitions))
    }

    @Test
    fun sourceAndTagFiltersUseAndCondition() {
        assertTrue(
            article.matchesNotificationFilter(
                setOf("Android Developers"),
                setOf("セキュリティ"),
                defaultTagDefinitions,
            ),
        )
        assertFalse(
            article.matchesNotificationFilter(
                setOf("Qiita"),
                setOf("セキュリティ"),
                defaultTagDefinitions,
            ),
        )
        assertFalse(
            article.matchesNotificationFilter(
                setOf("Android Developers"),
                setOf("データベース"),
                defaultTagDefinitions,
            ),
        )
    }
}
