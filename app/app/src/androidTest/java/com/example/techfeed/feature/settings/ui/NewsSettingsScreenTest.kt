package com.example.techfeed.feature.settings.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.techfeed.feature.settings.model.moveInOrder
import com.example.techfeed.feature.news.ui.CustomFeedState
import com.example.techfeed.feature.news.ui.CustomTagState
import com.example.techfeed.feature.translation.model.TranslationModelState
import com.example.techfeed.testsupport.uiSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewsSettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sourceSwitchSendsNewValueAndReflectsUpdatedSettings() {
        val settings = mutableStateOf(uiSettings.copy(sourceOrder = listOf("Publickey")))
        compose.setContent {
            MaterialTheme {
                NewsSettingsScreen(
                    settings.value,
                    null,
                    setSource = { source, enabled ->
                        settings.value =
                            settings.value.copy(
                                sources =
                                    if (enabled) settings.value.sources + source
                                    else settings.value.sources - source,
                            )
                    },
                    setTag = { _, _ -> },
                    moveSource = { _, _ -> },
                    moveTag = { _, _ -> },
                    customFeedState = CustomFeedState(),
                    addCustomFeed = {},
                    removeCustomFeed = {},
                    modelState = TranslationModelState(ready = true),
                    downloadModel = {},
                    back = {},
                )
            }
        }
        compose.onNodeWithTag("setting_switch:Publickey").assertIsOn().performClick().assertIsOff()
        compose.runOnIdle { assertEquals(setOf("ITmedia NEWS"), settings.value.sources) }
    }

    @Test
    fun moveTagButtonChangesTheRenderedOrder() {
        val settings = mutableStateOf(uiSettings.copy(sourceOrder = emptyList()))
        compose.setContent {
            MaterialTheme {
                NewsSettingsScreen(
                    settings.value,
                    null,
                    setSource = { _, _ -> },
                    setTag = { _, _ -> },
                    moveSource = { _, _ -> },
                    moveTag = { tag, offset ->
                        settings.value =
                            settings.value.copy(
                                tagOrder = moveInOrder(settings.value.tagOrder, tag, offset),
                            )
                    },
                    customFeedState = CustomFeedState(),
                    addCustomFeed = {},
                    removeCustomFeed = {},
                    modelState = TranslationModelState(ready = true),
                    downloadModel = {},
                    back = {},
                )
            }
        }
        compose.onNodeWithTag("settings_list").performScrollToNode(hasContentDescription("AI を前へ"))
        compose.onNodeWithContentDescription("AI を前へ").performClick()
        compose.runOnIdle { assertEquals(listOf("AI", "セキュリティ"), settings.value.tagOrder) }
        val aiTop = compose.onNodeWithTag("setting_switch:AI").fetchSemanticsNode().boundsInRoot.top
        val securityTop =
            compose.onNodeWithTag("setting_switch:セキュリティ").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(aiTop < securityTop)
    }

    @Test
    fun settingsModelDownloadAlsoWaitsForConfirmation() {
        var downloads = 0
        compose.setContent {
            MaterialTheme {
                NewsSettingsScreen(
                    uiSettings.copy(sourceOrder = emptyList(), tagOrder = emptyList()),
                    null,
                    { _, _ -> },
                    { _, _ -> },
                    { _, _ -> },
                    { _, _ -> },
                    CustomFeedState(),
                    {},
                    {},
                    TranslationModelState(ready = false),
                    { downloads++ },
                    {},
                )
            }
        }
        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("翻訳モデルをダウンロード"))
        compose.onNodeWithText("翻訳モデルをダウンロード").performClick()
        compose.runOnIdle { assertEquals(0, downloads) }
        compose.onNodeWithText("ダウンロード", substring = false).performClick()
        compose.runOnIdle { assertEquals(1, downloads) }
    }


    @Test
    fun customFeedUrlIsSentForValidation() {
        var submitted = ""
        compose.setContent {
            MaterialTheme {
                NewsSettingsScreen(
                    settings = uiSettings.copy(sourceOrder = emptyList(), tagOrder = emptyList()),
                    error = null,
                    setSource = { _, _ -> },
                    setTag = { _, _ -> },
                    moveSource = { _, _ -> },
                    moveTag = { _, _ -> },
                    customFeedState = CustomFeedState(),
                    addCustomFeed = { submitted = it },
                    removeCustomFeed = {},
                    modelState = TranslationModelState(ready = true),
                    downloadModel = {},
                    back = {},
                )
            }
        }

        compose.onNodeWithTag("custom_feed_url").performTextInput("https://example.com/feed.xml")
        compose.onNodeWithTag("add_custom_feed").performClick()
        compose.runOnIdle { assertEquals("https://example.com/feed.xml", submitted) }
    }

    @Test
    fun customTagNameAndKeywordsAreSubmitted() {
        var submittedName = ""
        var submittedKeywords = ""
        compose.setContent {
            MaterialTheme {
                NewsSettingsScreen(
                    settings = uiSettings.copy(sourceOrder = emptyList(), tagOrder = emptyList()),
                    error = null,
                    setSource = { _, _ -> },
                    setTag = { _, _ -> },
                    moveSource = { _, _ -> },
                    moveTag = { _, _ -> },
                    customFeedState = CustomFeedState(),
                    addCustomFeed = {},
                    removeCustomFeed = {},
                    modelState = TranslationModelState(ready = true),
                    downloadModel = {},
                    back = {},
                    customTagState = CustomTagState(),
                    addCustomTag = { name, keywords ->
                        submittedName = name
                        submittedKeywords = keywords
                    },
                )
            }
        }

        compose.onNodeWithTag("custom_tag_name").performTextInput("Compose")
        compose.onNodeWithTag("custom_tag_keywords").performTextInput("Jetpack Compose, Kotlin")
        compose.onNodeWithTag("add_custom_tag").performClick()
        compose.runOnIdle {
            assertEquals("Compose", submittedName)
            assertEquals("Jetpack Compose, Kotlin", submittedKeywords)
        }
    }
}
