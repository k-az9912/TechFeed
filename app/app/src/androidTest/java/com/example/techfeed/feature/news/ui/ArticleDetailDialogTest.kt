package com.example.techfeed.feature.news.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.techfeed.R
import com.example.techfeed.core.ui.UiText
import com.example.techfeed.feature.translation.model.ArticleTranslation
import com.example.techfeed.feature.translation.model.TranslationModelState
import com.example.techfeed.testsupport.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleDetailDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun modelDownloadRequiresConfirmationAndCancelDoesNotStartIt() {
        var downloads = 0
        compose.setContent {
            MaterialTheme {
                ArticleDetailDialog(
                    englishArticle,
                    ArticleTranslation(),
                    TranslationModelState(ready = false),
                    downloadModel = { downloads++ },
                    translate = {},
                    dismiss = {},
                )
            }
        }
        compose.onNodeWithText("Google で翻訳").performClick()
        compose.onNodeWithText("翻訳モデルをダウンロード").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, downloads) }
        compose.onNodeWithText("キャンセル").performClick()
        compose.runOnIdle { assertEquals(0, downloads) }

        compose.onNodeWithText("Google で翻訳").performClick()
        compose.onNodeWithText("ダウンロード", substring = false).performClick()
        compose.runOnIdle { assertEquals(1, downloads) }
        compose.onNodeWithText("翻訳モデルをダウンロード").assertDoesNotExist()
    }

    @Test
    fun downloadedModelTranslatesWithoutPromptAndUsesOneToggleButton() {
        var translations = 0
        compose.setContent {
            var translation by remember { mutableStateOf(ArticleTranslation()) }
            MaterialTheme {
                ArticleDetailDialog(
                    englishArticle,
                    translation,
                    TranslationModelState(ready = true),
                    downloadModel = { error("Downloaded model must not request download") },
                    translate = {
                        translations++
                        translation = ArticleTranslation(title = "日本語の見出し", summary = "日本語の概要")
                    },
                    dismiss = {},
                )
            }
        }
        compose.onNodeWithText("Google で翻訳").performClick()
        compose.onNodeWithText("翻訳モデルをダウンロード").assertDoesNotExist()
        compose.onNodeWithText("日本語の見出し").assertIsDisplayed()
        compose.onNodeWithText("Google で翻訳").assertDoesNotExist()
        compose.onNodeWithText("原文を表示").performClick()
        compose.onNodeWithText(englishArticle.title).assertIsDisplayed()
        compose.onNodeWithText("日本語訳を表示").performClick()
        compose.onNodeWithText("日本語の見出し").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, translations) }
    }

    @Test
    fun japaneseArticleDoesNotOfferTranslation() {
        compose.setContent {
            MaterialTheme {
                ArticleDetailDialog(
                    publickeySecurity,
                    ArticleTranslation(),
                    TranslationModelState(ready = true),
                    {},
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithText(publickeySecurity.title).assertIsDisplayed()
        compose.onNodeWithText("Google で翻訳").assertDoesNotExist()
    }

    @Test
    fun translationFailureKeepsOriginalAndAllowsRetry() {
        var state by mutableStateOf(ArticleTranslation())
        var attempts = 0
        compose.setContent {
            MaterialTheme {
                ArticleDetailDialog(
                    englishArticle,
                    state,
                    TranslationModelState(ready = true),
                    {},
                    {
                        attempts++
                        state = ArticleTranslation(loading = true)
                    },
                    {},
                )
            }
        }
        compose.onNodeWithText("Google で翻訳").performClick()
        compose.onNodeWithText("翻訳中…").assertIsNotEnabled()
        compose.runOnIdle { state = ArticleTranslation(error = UiText(R.string.error_translate)) }
        compose.onNodeWithText(englishArticle.title).assertIsDisplayed()
        compose.onNodeWithText("翻訳できませんでした。", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Google で翻訳").assertIsEnabled().performClick()
        compose.runOnIdle { state = ArticleTranslation(title = "再試行した翻訳", summary = "概要") }
        compose.onNodeWithText("再試行した翻訳").assertIsDisplayed()
        compose.onNodeWithText("翻訳できませんでした。", substring = true).assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, attempts) }
    }

    @Test
    fun modelDownloadFailureAllowsConfirmationAndRetry() {
        var model by mutableStateOf(TranslationModelState(ready = false))
        var attempts = 0
        compose.setContent {
            MaterialTheme {
                ArticleDetailDialog(
                    englishArticle,
                    ArticleTranslation(),
                    model,
                    {
                        attempts++
                        model = TranslationModelState(ready = false, downloading = true)
                    },
                    {},
                    {},
                )
            }
        }
        fun confirm() {
            compose.onNodeWithText("Google で翻訳").performClick()
            compose.onNodeWithText("ダウンロード", substring = false).performClick()
        }
        confirm()
        compose.runOnIdle {
            model =
                TranslationModelState(ready = false, error = UiText(R.string.error_model_download))
        }
        compose.onNodeWithText("モデルを取得できませんでした。", substring = true).assertIsDisplayed()
        confirm()
        compose.runOnIdle { model = TranslationModelState(ready = true) }
        compose.onNodeWithText("モデルを取得できませんでした。", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Google で翻訳").assertIsEnabled()
        compose.runOnIdle { assertEquals(2, attempts) }
    }
}
