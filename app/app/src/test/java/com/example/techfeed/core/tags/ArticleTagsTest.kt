package com.example.techfeed.core.tags

import com.example.techfeed.core.feed.availableSources
import com.example.techfeed.core.model.Article
import org.junit.Assert.*
import org.junit.Test

class ArticleTagsTest {
    private fun article(source: String = "Test", title: String = "", summary: String = "") =
        Article("https://example.com/a", source, "ITニュース", title, summary, "")

    @Test
    fun securityAndAiCanCoexistWithSourceCategory() {
        val tags = articleTags(article(title = "AI サービスの脆弱性 CVE-2026-1234"))
        assertTrue(tags.containsAll(setOf("ITニュース", "AI", "セキュリティ")))
    }

    @Test
    fun englishSummaryCanAddSecurityTag() {
        assertTrue("セキュリティ" in articleTags(article(summary = "A vulnerability allows data theft")))
    }

    @Test
    fun ordinaryArticleDoesNotReceiveSecurityTag() {
        assertFalse("セキュリティ" in articleTags(article(title = "New laptop released")))
    }

    @Test
    fun ipaSecuritySourceIsTaggedWithoutKeywords() {
        assertTrue("セキュリティ" in articleTags(article(source = "IPA セキュリティ")))
    }

    @Test
    fun sourceNamesAreNotTopicTags() {
        assertFalse(availableTags.any { it in availableSources })
        assertFalse("Qiitaトレンド" in availableTags)
        assertFalse("海外IT" in availableTags)
        val article = Article("1", "Qiita", "Qiitaトレンド", "Compose", "", "")
        assertEquals(setOf("プログラミング"), articleTags(article))
    }

    @Test
    fun expandedDefaultCategoriesMatchCommonTechnologyTopics() {
        val tags =
            articleTags(
                article(
                    title = "Kubernetes and PostgreSQL backend API",
                    summary = "Docker cloud deployment",
                ),
            )
        assertTrue(tags.containsAll(setOf("クラウド・インフラ", "バックエンド・API", "データベース")))
    }

    @Test
    fun customTagUsesCaseInsensitiveKeywords() {
        val custom = TagDefinition("Compose", listOf("jetpack compose"), custom = true)
        assertEquals(setOf("Compose"), articleTags(article(title = "JETPACK COMPOSE 1.0"), listOf(custom)))
    }

    @Test
    fun shortAsciiKeywordDoesNotMatchInsideAnotherWord() {
        assertFalse("AI" in articleTags(article(title = "Paid plan update")))
    }
}
