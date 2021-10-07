package xyz.aprildown.timer.app.analytics

import android.app.Application
import android.content.Context
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.crashes.Crashes
import dagger.Reusable
import xyz.aprildown.timer.analytics.BuildConfig
import xyz.aprildown.timer.domain.utils.AppTracker
import javax.inject.Inject

@Reusable
internal class AppTrackerImpl @Inject constructor() : AppTracker {
    override fun init(context: Context) {
        Firebase.analytics.setAnalyticsCollectionEnabled(true)
        AppCenter.start(
            context.applicationContext as Application,
            BuildConfig.APP_CENTER_APP_SECRET,
            Crashes::class.java
        )
    }

    override fun trackEvent(event: String, property: String?, value: String?) {
        Firebase.analytics.logEvent(event) {
            if (property != null && value != null) {
                param(property, value)
            }
        }
    }

    override fun trackError(throwable: Throwable, message: String?) {
        if (message != null) {
            Crashes.trackError(IllegalStateException(message, throwable))
        } else {
            Crashes.trackError(throwable)
        }
    }

    override suspend fun hasCrashedInLastSession(): Boolean {
        return Crashes.hasCrashedInLastSession().get()
    }
}
