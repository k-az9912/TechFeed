package com.example.techfeed.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun parseArticleTime(value: String): Instant? =
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
        }
            .getOrNull()

fun formatArticleTime(
    value: String,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String? {
    val instant = parseArticleTime(value)
    if (instant != null) {
        return DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", locale)
            .withZone(zone)
            .format(instant)
    }
    // A date without a time zone remains a date; do not invent a publication time.
    return runCatching {
        LocalDate.parse(value).format(DateTimeFormatter.ofPattern("yyyy/MM/dd", locale))
    }
        .getOrNull()
}
