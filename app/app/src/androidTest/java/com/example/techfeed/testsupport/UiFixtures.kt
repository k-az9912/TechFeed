package com.example.techfeed.testsupport

import com.example.techfeed.core.model.Article
import com.example.techfeed.feature.settings.model.NewsSettings

val publickeySecurity = Article(
    "https://example.com/security", "Publickey", "開発",
    "テスト用の脆弱性ニュース", "セキュリティの概要", "2026-10-28"
)
val publickeyAi = Article(
    "https://example.com/ai", "Publickey", "開発",
    "テスト用の生成AIニュース", "AI の概要", "2026-10-28"
)
val itmediaSecurity = Article(
    "https://example.com/other", "ITmedia NEWS", "ITニュース",
    "別サイトの脆弱性ニュース", "セキュリティの概要", "2026-10-28"
)
val englishArticle = Article(
    "https://example.com/english", "Ars Technica", "海外IT",
    "English headline", "English summary", "2026-10-28"
)
val uiSettings = NewsSettings(
    sources = setOf("Publickey", "ITmedia NEWS"),
    visibleTags = setOf("セキュリティ", "AI"),
    sourceOrder = listOf("Publickey", "ITmedia NEWS"),
    tagOrder = listOf("セキュリティ", "AI")
)
