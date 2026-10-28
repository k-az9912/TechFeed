package com.example.techfeed.feature.news.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.techfeed.core.model.Article
import com.example.techfeed.testsupport.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sourceAndTagFiltersNarrowTheArticleList() {
        compose.setContent {
            MaterialTheme {
                NewsScreen(
                    state = NewsState(listOf(publickeySecurity, publickeyAi, itmediaSecurity)),
                    settings = uiSettings
                )
            }
        }
        compose.onNodeWithTag("filter:取得元:Publickey").performClick()
        compose.onNodeWithTag("filter:タグ:セキュリティ").performClick()

        compose.onNodeWithText(publickeySecurity.title).assertIsDisplayed()
        compose.onNodeWithText(publickeyAi.title).assertDoesNotExist()
        compose.onNodeWithText(itmediaSecurity.title).assertDoesNotExist()
    }

    @Test fun saveButtonUpdatesSavedListAndCanRemoveArticle() {
        compose.setContent {
            var saved by remember { mutableStateOf(emptyList<Article>()) }
            MaterialTheme {
                NewsScreen(
                    state = NewsState(listOf(publickeySecurity)),
                    saved = saved,
                    settings = uiSettings,
                    toggleSaved = { article ->
                        saved = if (saved.any { it.id == article.id }) emptyList() else listOf(article)
                    }
                )
            }
        }
        compose.onNodeWithText("☆ 保存").performClick()
        compose.onNodeWithText("★ 保存済み").assertIsDisplayed()
        compose.onNodeWithContentDescription("保存済み記事").performClick()
        compose.onNodeWithText(publickeySecurity.title).assertIsDisplayed()
        compose.onNodeWithText("★ 保存済み").performClick()
        compose.onNodeWithText("記事がありません").assertIsDisplayed()
        compose.onNodeWithText(publickeySecurity.title).assertDoesNotExist()
    }

    @Test fun espressoBackClosesArticleDialogWithoutLeavingList() {
        compose.setContent {
            MaterialTheme {
                NewsScreen(state = NewsState(listOf(publickeySecurity)), settings = uiSettings)
            }
        }
        compose.onNodeWithText(publickeySecurity.title).performClick()
        compose.onNode(isDialog()).assertIsDisplayed()

        // Espresso sends the Android Back action; Compose assertions verify the result.
        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithText(publickeySecurity.title).assertIsDisplayed()
    }

    @Test fun scrollingHidesFiltersAndReverseScrollShowsThem() {
        val articles = (1..30).map {
            publickeySecurity.copy(id = "https://example.com/$it", title = "テスト記事 $it")
        }
        compose.setContent {
            MaterialTheme { NewsScreen(state = NewsState(articles), settings = uiSettings) }
        }
        compose.onNodeWithTag("news_filters").assertIsDisplayed()
        compose.onNodeWithTag("news_list").performTouchInput { swipeUp() }
        compose.onNodeWithTag("news_filters").assertDoesNotExist()
        compose.onNodeWithTag("news_list").performTouchInput { swipeDown() }
        compose.onNodeWithTag("news_filters").assertIsDisplayed()
    }
}
