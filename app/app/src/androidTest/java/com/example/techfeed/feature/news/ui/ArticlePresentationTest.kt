package com.example.techfeed.feature.news.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.techfeed.R
import com.example.techfeed.core.ui.UiText
import com.example.techfeed.feature.news.ui.components.ArticleCard
import com.example.techfeed.testsupport.publickeySecurity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticlePresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cardDisplaysDateWithoutInventingTime() {
        compose.setContent {
            MaterialTheme {
                ArticleCard(
                    article = publickeySecurity.copy(time = "2026-10-29"),
                    tags = emptyList(),
                    saved = false,
                    openArticle = {},
                    toggleSaved = {},
                )
            }
        }
        compose.onNodeWithText("Publickey · 2026/10/29").assertIsDisplayed()
    }

    @Test
    fun cardUsesFallbackForInvalidDate() {
        compose.setContent {
            MaterialTheme {
                ArticleCard(
                    article = publickeySecurity.copy(time = "invalid"),
                    tags = emptyList(),
                    saved = false,
                    openArticle = {},
                    toggleSaved = {},
                )
            }
        }
        compose.onNodeWithText("Publickey · 日時不明").assertIsDisplayed()
    }

    @Test
    fun errorResourceIncludesFailedSourceNames() {
        compose.setContent {
            Text(UiText(R.string.error_feed_fetch, listOf("Publickey, Zenn")).resolve())
        }
        compose.onNodeWithText("取得できませんでした：Publickey, Zenn。再試行してください。").assertIsDisplayed()
    }
}
