package com.example.techfeed.feature.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.techfeed.core.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleNotifierTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun allowNotificationsOnAndroid13AndLater() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun postsNewArticleNotificationToAndroid() {
        manager.cancelAll()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                assertEquals(
                    PackageManager.PERMISSION_GRANTED,
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS),
                )
            }
            ArticleNotifier(context)
                .notify(
                    listOf(
                        Article(
                            id = "https://example.com/new-article",
                            source = "TechFeed テスト",
                            category = "ITニュース",
                            title = "新着記事の通知テストに成功しました",
                            summary = "通知表示をAndroid上で確認するためのテスト記事です。",
                            time = "2026-11-04T12:00:00+09:00",
                        ),
                    ),
                )

            var posted = manager.activeNotifications.singleOrNull { it.id == 1001 }
            repeat(20) {
                if (posted != null) return@repeat
                Thread.sleep(50)
                posted = manager.activeNotifications.singleOrNull { it.id == 1001 }
            }
            assertNotNull("Androidに通知が投稿されていません", posted)
            assertEquals(
                "TechFeed テストから新着記事",
                posted?.notification?.extras?.getString("android.title"),
            )
            assertEquals(
                "新着記事の通知テストに成功しました",
                posted?.notification?.extras?.getString("android.text"),
            )
        } finally {
            val keepNotification =
                InstrumentationRegistry.getArguments().getString("keepNotification") == "true"
            if (!keepNotification) manager.cancelAll()
        }
    }
}
