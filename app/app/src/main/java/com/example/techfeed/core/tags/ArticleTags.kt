package com.example.techfeed.core.tags

import com.example.techfeed.core.model.Article

val availableTags = listOf("セキュリティ", "AI", "Android", "開発", "ITニュース", "お知らせ")

fun articleTags(article: Article): Set<String> {
    val text = "${article.title} ${article.summary}".lowercase(java.util.Locale.ROOT)
    return buildSet {
        when (article.category) {
            in availableTags -> add(article.category)
            "Qiitaトレンド",
            "Zennトレンド" -> add("開発")
            "IPA" -> add("お知らせ")
            "海外IT" -> add("ITニュース")
        }
        if (
            article.source == "IPA セキュリティ" ||
                listOf(
                        "セキュリティ",
                        "脆弱性",
                        "漏えい",
                        "漏洩",
                        "ランサム",
                        "不正アクセス",
                        "security",
                        "vulnerab",
                        "cve-",
                        "ransomware",
                        "malware",
                        "data breach",
                    )
                    .any { it in text }
        )
            add("セキュリティ")
        if (Regex("(?i)\\b(ai|llm|gpt)\\b|人工知能|生成AI").containsMatchIn(text)) add("AI")
    }
}
