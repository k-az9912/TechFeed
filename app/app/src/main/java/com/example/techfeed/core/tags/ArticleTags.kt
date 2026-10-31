package com.example.techfeed.core.tags

import com.example.techfeed.core.model.Article
import java.util.Locale

data class TagDefinition(
    val name: String,
    val keywords: List<String> = emptyList(),
    val categories: Set<String> = emptySet(),
    val sources: Set<String> = emptySet(),
    val custom: Boolean = false,
)

val defaultTagDefinitions =
    listOf(
        TagDefinition(
            "セキュリティ",
            listOf(
                "セキュリティ", "脆弱性", "漏えい", "漏洩", "ランサム", "不正アクセス",
                "security", "vulnerability", "cve-", "ransomware", "malware", "data breach",
            ),
            categories = setOf("セキュリティ"),
            sources = setOf("IPA セキュリティ"),
        ),
        TagDefinition(
            "AI",
            listOf(
                "AI", "LLM", "GPT", "人工知能", "生成AI", "機械学習", "machine learning",
                "deep learning",
            ),
        ),
        TagDefinition(
            "Android",
            listOf("Android", "Jetpack Compose", "Android Studio"),
            categories = setOf("Android"),
            sources = setOf("Android Developers"),
        ),
        TagDefinition(
            "Web",
            listOf(
                "Web", "フロントエンド", "frontend", "JavaScript", "TypeScript", "React",
                "Vue", "Next.js", "CSS", "HTML",
            ),
        ),
        TagDefinition(
            "モバイル",
            listOf(
                "モバイル", "mobile", "iOS", "Swift", "Flutter", "React Native",
                "Kotlin Multiplatform",
            ),
        ),
        TagDefinition(
            "クラウド・インフラ",
            listOf(
                "クラウド", "インフラ", "cloud", "AWS", "Azure", "Google Cloud",
                "Kubernetes", "Docker", "Terraform", "サーバーレス", "serverless",
            ),
        ),
        TagDefinition(
            "バックエンド・API",
            listOf(
                "バックエンド", "backend", "API", "サーバーサイド", "Spring Boot", "Node.js",
                "Ruby on Rails", "Django", "GraphQL",
            ),
        ),
        TagDefinition(
            "データベース",
            listOf(
                "データベース", "database", "SQL", "PostgreSQL", "MySQL", "SQLite", "Room",
                "Redis", "MongoDB",
            ),
        ),
        TagDefinition(
            "開発ツール",
            listOf(
                "開発ツール", "GitHub", "GitLab", "Git", "Gradle", "IDE", "CI/CD", "DevOps",
                "テスト自動化",
            ),
        ),
        TagDefinition(
            "プログラミング",
            listOf(
                "プログラミング", "programming", "Kotlin", "Java", "Python", "Rust",
                "コンパイラ", "アルゴリズム",
            ),
            categories = setOf("開発", "Qiitaトレンド", "Zennトレンド"),
        ),
        TagDefinition("ITニュース", categories = setOf("ITニュース", "海外IT")),
        TagDefinition("お知らせ", categories = setOf("IPA")),
    )

val availableTags = defaultTagDefinitions.map { it.name }

fun articleTags(
    article: Article,
    definitions: List<TagDefinition> = defaultTagDefinitions,
): Set<String> {
    val text = "${article.title} ${article.summary}".lowercase(Locale.ROOT)
    return definitions
        .filter { definition ->
            article.category in definition.categories ||
                article.source in definition.sources ||
                definition.keywords.any { matchesKeyword(text, it) }
        }
        .mapTo(linkedSetOf()) { it.name }
}

private fun matchesKeyword(text: String, keyword: String): Boolean {
    val normalized = keyword.trim().lowercase(Locale.ROOT)
    if (normalized.isEmpty()) return false
    if (normalized.any { it.code > 127 }) return normalized in text
    return Regex("(?<![a-z0-9])${Regex.escape(normalized)}(?![a-z0-9])")
        .containsMatchIn(text)
}
