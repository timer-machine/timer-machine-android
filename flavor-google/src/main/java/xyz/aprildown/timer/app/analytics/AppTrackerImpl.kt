package xyz.aprildown.timer.app.analytics

import android.content.Context
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.Reusable
import xyz.aprildown.timer.domain.utils.AppTracker
import javax.inject.Inject

@Reusable
internal class AppTrackerImpl @Inject constructor() : AppTracker {
    override fun init(context: Context) = Unit

    override fun trackError(throwable: Throwable, message: String?) {
        if (message != null) {
            Firebase.crashlytics.recordException(IllegalStateException(message, throwable))
        } else {
            Firebase.crashlytics.recordException(throwable)
        }
    }

    override suspend fun hasCrashedInLastSession(): Boolean {
        return Firebase.crashlytics.didCrashOnPreviousExecution()
    }
}
