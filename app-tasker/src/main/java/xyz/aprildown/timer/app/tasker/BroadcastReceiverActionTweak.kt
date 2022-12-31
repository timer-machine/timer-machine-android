package xyz.aprildown.timer.app.tasker

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.joaomgcd.taskerpluginlibrary.TaskerPluginConstants
import com.joaomgcd.taskerpluginlibrary.action.BroadcastReceiverAction
import com.joaomgcd.taskerpluginlibrary.action.IntentServiceAction

/**
 * Hack around to provide background compatibility.
 * From [BroadcastReceiverAction].
 */
internal class BroadcastReceiverActionTweak : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // resultCode = TaskerPlugin.Setting.RESULT_CODE_PENDING
        try {
            if (intent == null || context == null) return
            intent.component = ComponentName(context, IntentServiceAction::class.java)

            intent.getBundleExtra(
                TaskerPluginConstants.EXTRA_BUNDLE
            )?.apply {
                putString(
                    TaskerPluginConstants.EXTRA_ACTION_RUNNER_CLASS,
                    TaskerTimerRunner::class.java.name
                )
                putString(
                    TaskerPluginConstants.EXTRA_ACTION_INPUT_CLASS,
                    Unit::class.java.name
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // This requires no Battery Optimization at least Android 12
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {
        }
    }
}
