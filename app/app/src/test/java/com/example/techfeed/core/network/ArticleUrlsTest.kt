package com.example.techfeed.core.network

import org.junit.Assert.*
import org.junit.Test

class ArticleUrlsTest {

    @Test
    fun unsafeSchemesAreRejected() {
        assertNull(safeArticleUrl("file:///etc/passwd"))
        assertNull(safeArticleUrl("https://user:password@example.com/"))
        assertEquals("https://example.com/a", safeArticleUrl("https://example.com/a"))
    }
}
