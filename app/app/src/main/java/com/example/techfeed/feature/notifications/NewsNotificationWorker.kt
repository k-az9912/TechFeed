package com.example.techfeed.feature.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.techfeed.core.model.Article
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope

class NewsNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val dependencies =
            EntryPointAccessors.fromApplication(
                applicationContext,
                NotificationWorkerEntryPoint::class.java,
            )
        val settingsRepository = dependencies.settingsRepository()
        return try {
            val settings = settingsRepository.settings.first()
            if (!settings.notificationsEnabled) return Result.success()
            val enabledFeeds = settings.feeds.filter { it.name in settings.sources }
            val results =
                supervisorScope {
                    enabledFeeds
                        .map { source ->
                            async {
                                try {
                                    dependencies.newsRepository().fetch(source)
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (_: Exception) {
                                    null
                                }
                            }
                        }
                        .awaitAll()
                }
            val successful = results.filterNotNull()
            if (enabledFeeds.isNotEmpty() && successful.isEmpty()) return Result.retry()
            val articles = successful.flatten().distinctBy(Article::id)
            val newIds =
                settingsRepository.updateNotificationHistory(
                    articleIds = articles.map(Article::id),
                    allowInitialization = successful.size == enabledFeeds.size,
                )
            val notificationArticles =
                articles.filter { article ->
                    article.id in newIds &&
                        article.matchesNotificationFilter(
                            sources = settings.notificationSources,
                            tags = settings.notificationTags,
                            tagDefinitions = settings.tags,
                        )
                }
            ArticleNotifier(applicationContext).notify(notificationArticles)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "news_notifications"
    }
}
