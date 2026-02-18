package xyz.aprildown.timer.app.broadcast

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class TimerBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var presenter: BroadcastPresenter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BroadcastConstants.ACTION_TIMER_CONTROL) return

        try {
            val serviceIntent = runBlocking { presenter.handleIntent(intent) }
                ?: return

            if (presenter.requiresRunningService(intent)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val nm = context.getSystemService<NotificationManager>()
                    if (nm == null || nm.activeNotifications.isEmpty()) return
                }
            }

            // On Android 12+, startForegroundService from the background requires
            // battery optimization to be disabled for this app. Same limitation
            // as the Tasker integration (see BroadcastReceiverActionTweak).
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle broadcast timer control", e)
        }
    }

    companion object {
        private const val TAG = "TimerBroadcastReceiver"
    }
}
