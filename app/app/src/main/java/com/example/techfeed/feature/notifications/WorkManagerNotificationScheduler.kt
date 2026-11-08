package com.example.techfeed.feature.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerNotificationScheduler
@Inject
constructor(@param:ApplicationContext private val context: Context) : NotificationScheduler {
    override fun sync(enabled: Boolean, intervalMinutes: Int) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(NewsNotificationWorker.UNIQUE_WORK_NAME)
            return
        }
        val request =
            PeriodicWorkRequestBuilder<NewsNotificationWorker>(
                    intervalMinutes.toLong(),
                    TimeUnit.MINUTES,
                )
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .build()
        manager.enqueueUniquePeriodicWork(
            NewsNotificationWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
