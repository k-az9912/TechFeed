package com.example.techfeed.core.time

import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class ArticleTimeTest {
    private val tokyo = ZoneId.of("Asia/Tokyo")

    @Test
    fun isoAndRssDatesUseTheSameDisplayFormat() {
        val iso = formatArticleTime("2026-10-29T01:30:00Z", tokyo, Locale.JAPAN)
        val rss = formatArticleTime("Thu, 29 Oct 2026 10:30:00 +0900", tokyo, Locale.JAPAN)
        assertEquals("2026/10/29 10:30", iso)
        assertEquals(iso, rss)
    }

    @Test
    fun displayUsesRequestedTimeZoneAcrossDateBoundaries() {
        assertEquals(
            "2026/10/30 01:00",
            formatArticleTime("2026-10-29T16:00:00Z", tokyo, Locale.JAPAN),
        )
        assertEquals(
            "2026/10/29 09:00",
            formatArticleTime("2026-10-29T16:00:00Z", ZoneId.of("America/Los_Angeles"), Locale.US),
        )
    }

    @Test
    fun dateOnlyDoesNotInventTimeOfDay() {
        assertEquals("2026/10/29", formatArticleTime("2026-10-29", tokyo, Locale.JAPAN))
    }

    @Test
    fun emptyOrInvalidDatesHaveNoDisplayValue() {
        assertNull(formatArticleTime("", tokyo, Locale.JAPAN))
        assertNull(formatArticleTime("invalid", tokyo, Locale.JAPAN))
        assertNull(formatArticleTime("2026-02-30", tokyo, Locale.JAPAN))
    }
}
