package com.example.techfeed.core.feed

import com.example.techfeed.core.model.Article
import org.junit.Assert.*
import org.junit.Test

class FeedSourcesTest {
    private fun article(source: String = "Test", title: String = "", summary: String = "") =
        Article("https://example.com/a", source, "ITニュース", title, summary, "")

    @Test
    fun translationButtonTargetsEnglishSourcesOnly() {
        assertTrue(isEnglishArticle(article(source = "Ars Technica")))
        assertTrue(isEnglishArticle(article(source = "Android Developers")))
        assertFalse(isEnglishArticle(article(source = "IPA お知らせ")))
        assertFalse(isEnglishArticle(article(source = "Qiita")))
    }
}
