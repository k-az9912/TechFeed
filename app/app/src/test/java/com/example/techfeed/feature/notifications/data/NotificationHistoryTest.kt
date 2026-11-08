package com.example.techfeed.feature.notifications.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHistoryTest {
    @Test
    fun firstCheckCreatesBaselineWithoutNewArticles() {
        val update =
            calculateNotificationHistory(
                articleIds = listOf("https://example.com/1", "https://example.com/2"),
                previousHashes = emptySet(),
                initialized = false,
            )

        assertTrue(update.newArticleIds.isEmpty())
        assertEquals(2, update.storedHashes.size)
    }

    @Test
    fun laterCheckReturnsOnlyPreviouslyUnseenArticles() {
        val baseline =
            calculateNotificationHistory(
                articleIds = listOf("https://example.com/1"),
                previousHashes = emptySet(),
                initialized = false,
            )
        val update =
            calculateNotificationHistory(
                articleIds = listOf("https://example.com/1", "https://example.com/2"),
                previousHashes = baseline.storedHashes,
                initialized = true,
            )

        assertEquals(listOf("https://example.com/2"), update.newArticleIds)
        assertEquals(2, update.storedHashes.size)
    }
}
