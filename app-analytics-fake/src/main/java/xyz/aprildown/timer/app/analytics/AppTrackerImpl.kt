package xyz.aprildown.timer.app.analytics

import android.content.Context
import dagger.Reusable
import timber.log.Timber
import xyz.aprildown.timer.domain.utils.AppTracker
import javax.inject.Inject

@Reusable
internal class AppTrackerImpl @Inject constructor() : AppTracker {
    override fun init(context: Context) = Unit

    override fun trackError(throwable: Throwable, message: String?) {
        if (message != null) {
            Timber.e(throwable, message)
        } else {
            Timber.e(throwable)
        }
    }

    override suspend fun hasCrashedInLastSession(): Boolean {
        return false
    }
}
