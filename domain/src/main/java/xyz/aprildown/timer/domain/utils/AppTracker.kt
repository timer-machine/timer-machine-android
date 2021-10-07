package xyz.aprildown.timer.domain.utils

import android.content.Context

interface AppTracker {
    fun init(context: Context)
    fun trackEvent(event: String, property: String? = null, value: String? = null)
    fun trackError(throwable: Throwable, message: String? = null)
    suspend fun hasCrashedInLastSession(): Boolean
}
