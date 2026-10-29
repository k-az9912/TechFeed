package com.example.techfeed.feature.news.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.techfeed.core.ui.theme.TechFeedTheme
import com.example.techfeed.feature.news.ui.components.ArticleCard
import com.example.techfeed.feature.news.ui.components.ArticleFilterRow
import com.example.techfeed.testsupport.englishArticle
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class AdaptiveUiTest(private val width: Int, private val scale: Float, private val dark: Boolean) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longArticleRemainsReadableAndSaveAndFilterWork() {
        val title = "Android の新機能とセキュリティ更新について詳しく紹介する長い記事の見出し"
        var saves = 0
        var selected = "すべて"
        var background = 1f
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, 820.dp)),
            ) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(scale)) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.DarkMode(dark)) {
                        TechFeedTheme {
                            background = MaterialTheme.colorScheme.background.luminance()
                            androidx.compose.material3.Surface {
                                Column(Modifier.fillMaxSize()) {
                                    ArticleFilterRow("取得元", listOf("すべて", "Qiita"), "すべて") {
                                        selected = it
                                    }
                                    ArticleCard(
                                        englishArticle.copy(title = title, summary = "記事の概要"),
                                        listOf("Android", "セキュリティ", "開発"),
                                        false,
                                        {},
                                        { saves++ },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Qiita").performClick()
        compose.onNodeWithText("☆ 保存").assertIsDisplayed().performClick()
        for (text in listOf(title, "☆ 保存", "取得元")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).performSemanticsAction(
                SemanticsActions.GetTextLayoutResult,
            ) {
                it(layouts)
            }
            assertTrue("No text layout: $text", layouts.isNotEmpty())
            // TextLayoutResult uses integer bounds; allow at most one pixel of rounding.
            assertFalse(
                "Clipped text: $text ($width/$scale/$dark)",
                layouts.any {
                    it.didOverflowHeight ||
                        it.multiParagraph.didExceedMaxLines ||
                        (0 until it.lineCount).any { line ->
                            it.getLineRight(line) > it.size.width + 1f
                        }
                },
            )
        }
        compose.runOnIdle {
            assertEquals(1, saves)
            assertEquals("Qiita", selected)
            assertEquals(dark, background < 0.5f)
        }
        val folder =
            File(
                    InstrumentationRegistry.getInstrumentation()
                        .targetContext
                        .getExternalFilesDir(null),
                    "ui-check",
                )
                .apply { mkdirs() }
        File(folder, "article-$width-$scale-$dark.png").outputStream().use {
            compose
                .onRoot()
                .captureToImage()
                .asAndroidBitmap()
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}, font={1}, dark={2}")
        fun configurations(): List<Array<Any>> =
            listOf(320, 411).flatMap { width ->
                listOf(1f, 2f).flatMap { scale ->
                    listOf(false, true).map { dark -> arrayOf<Any>(width, scale, dark) }
                }
            }
    }
}
