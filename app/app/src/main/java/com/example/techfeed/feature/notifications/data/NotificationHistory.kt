package com.example.techfeed.feature.notifications.data

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class NotificationHistoryUpdate(
    val newArticleIds: List<String>,
    val storedHashes: Set<String>,
)

fun calculateNotificationHistory(
    articleIds: List<String>,
    previousHashes: Set<String>,
    initialized: Boolean,
    limit: Int = 1_000,
): NotificationHistoryUpdate {
    val distinctIds = articleIds.distinct()
    val current = distinctIds.associateWith(::articleIdHash)
    val newIds =
        if (initialized) distinctIds.filter { current.getValue(it) !in previousHashes }
        else emptyList()
    val stored = (current.values + previousHashes).distinct().take(limit).toSet()
    return NotificationHistoryUpdate(newIds, stored)
}

private fun articleIdHash(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
