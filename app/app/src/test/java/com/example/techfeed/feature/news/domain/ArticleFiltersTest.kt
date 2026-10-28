package com.example.techfeed.feature.news.domain

import com.example.techfeed.core.model.Article
import org.junit.Assert.*
import org.junit.Test

class ArticleFiltersTest {

    @Test fun sourceAndTopicFiltersAreCombined() {
        val securityA = Article("1", "A", "ITニュース", "脆弱性について", "", "")
        val securityB = securityA.copy(id = "2", source = "B")
        val normalA = securityA.copy(id = "3", title = "New laptop")
        val all = listOf(securityA, securityB, normalA)
        assertEquals(listOf(securityA), filterArticles(all, "A", "セキュリティ"))
        assertEquals(listOf(securityA, normalA), filterArticles(all, "A", "すべて"))
        assertEquals(listOf(securityA, securityB), filterArticles(all, "すべて", "セキュリティ"))
    }
}
