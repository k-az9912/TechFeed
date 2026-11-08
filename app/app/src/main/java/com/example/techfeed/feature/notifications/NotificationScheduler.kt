package com.example.techfeed.feature.notifications

interface NotificationScheduler {
    fun sync(enabled: Boolean, intervalMinutes: Int)

    object None : NotificationScheduler {
        override fun sync(enabled: Boolean, intervalMinutes: Int) = Unit
    }
}
