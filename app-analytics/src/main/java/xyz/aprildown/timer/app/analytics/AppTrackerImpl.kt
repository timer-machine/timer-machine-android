package xyz.aprildown.timer.app.analytics

import android.app.Application
import android.content.Context
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.crashes.Crashes
import dagger.Reusable
import xyz.aprildown.timer.analytics.BuildConfig
import xyz.aprildown.timer.domain.utils.AppTracker
import javax.inject.Inject

@Reusable
internal class AppTrackerImpl @Inject constructor() : AppTracker {
    override fun init(context: Context) {
        AppCenter.start(
            context.applicationContext as Application,
            BuildConfig.APP_CENTER_APP_SECRET,
            Crashes::class.java
        )
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
