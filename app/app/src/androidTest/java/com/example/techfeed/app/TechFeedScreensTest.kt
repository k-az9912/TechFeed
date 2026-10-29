package com.example.techfeed.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.techfeed.feature.news.ui.NewsScreen
import com.example.techfeed.feature.news.ui.NewsState
import com.example.techfeed.testsupport.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TechFeedScreensTest {
    @get:Rule val compose = createComposeRule()

    private fun setScreens() {
        val articles = (1..30).map {
            publickeySecurity.copy(id = "https://example.com/$it", title = "記事 $it")
        }
        compose.setContent {
            MaterialTheme {
                TechFeedScreens(
                    news = { openSettings ->
                        NewsScreen(
                            state = NewsState(articles + listOf(itmediaSecurity, publickeyAi)),
                            saved = articles + listOf(itmediaSecurity, publickeyAi),
                            settings = uiSettings,
                            openSettings = openSettings
                        )
                    },
                    settings = { back -> TextButton(onClick = back) { Text("一覧へ戻る") } }
                )
            }
        }
    }

    private fun scrollPosition(): Float = compose.onNodeWithTag("news_list")
        .fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

    @Test fun settingsRoundTripKeepsSavedTabFiltersAndScrollPosition() {
        setScreens()
        compose.onNodeWithContentDescription("保存済み記事").performClick()
        compose.onNodeWithTag("filter:取得元:Publickey").performClick()
        compose.onNodeWithTag("filter:タグ:セキュリティ").performClick()
        compose.onNodeWithTag("news_list").performTouchInput { swipeUp() }
        compose.waitForIdle()
        val before = scrollPosition()
        assertTrue(before > 0f)
        compose.onNodeWithTag("news_filters").assertDoesNotExist()

        compose.onNodeWithContentDescription("設定").performClick()
        compose.onNodeWithText("一覧へ戻る").performClick()
        compose.onNodeWithContentDescription("ニュース一覧").assertIsDisplayed()
        assertEquals(before, scrollPosition(), 0.1f)
        compose.onNodeWithTag("news_filters").assertDoesNotExist()
        compose.onNodeWithText(itmediaSecurity.title).assertDoesNotExist()
        compose.onNodeWithText(publickeyAi.title).assertDoesNotExist()

        compose.onNodeWithTag("news_list").performTouchInput { swipeDown() }
        compose.onNodeWithTag("filter:取得元:Publickey").assertIsSelected()
        compose.onNodeWithTag("filter:タグ:セキュリティ").assertIsSelected()
        compose.onNodeWithTag("filter:タグ:AI").performClick()
        compose.onNodeWithText(publickeyAi.title).assertIsDisplayed()
        assertEquals(0f, scrollPosition(), 0.1f)
    }

    @Test fun androidBackFromSettingsKeepsSourceSelection() {
        setScreens()
        compose.onNodeWithTag("filter:取得元:ITmedia NEWS").performClick()
        compose.onNodeWithContentDescription("設定").performClick()
        Espresso.pressBack()
        compose.onNodeWithTag("filter:取得元:ITmedia NEWS").assertIsSelected()
        compose.onNodeWithText(itmediaSecurity.title).assertIsDisplayed()
        compose.onNodeWithText(publickeyAi.title).assertDoesNotExist()
    }
}
