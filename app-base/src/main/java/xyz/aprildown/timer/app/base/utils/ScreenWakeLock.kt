package xyz.aprildown.timer.app.base.utils

import android.content.Context
import android.os.PowerManager
import androidx.core.content.getSystemService
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.tools.helper.safeSharedPreference

object ScreenWakeLock {
    private const val LOG_TAG = "TimeR Machine: Screen WakeLock"

    private var sScreenWakeLock: PowerManager.WakeLock? = null

    fun acquireScreenWakeLock(context: Context, screenTiming: String) {
        if (!isValidLocation(context, screenTiming)) return

        if (sScreenWakeLock == null) {
            val screenPref = context.safeSharedPreference.getString(
                PreferenceData.KEY_SCREEN,
                context.getString(R.string.pref_screen_value_default)
            )
            val level = when (screenPref) {
                context.getString(R.string.pref_screen_value_keep) -> {
                    @Suppress("DEPRECATION")
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                }
                context.getString(R.string.pref_screen_value_dim) -> {
                    @Suppress("DEPRECATION")
                    PowerManager.SCREEN_DIM_WAKE_LOCK
                }
                else -> return
            }
            sScreenWakeLock = context.getSystemService<PowerManager>()?.newWakeLock(level, LOG_TAG)
            sScreenWakeLock?.setReferenceCounted(true)
        }

        @Suppress("WakelockTimeout")
        sScreenWakeLock?.acquire()
    }

    private fun isValidLocation(context: Context, screenTiming: String): Boolean {
        return context.safeSharedPreference.getString(
            PreferenceData.KEY_SCREEN_TIMING,
            context.getString(R.string.pref_screen_timing_value_default)
        ) == screenTiming
    }

    fun releaseScreenLock(context: Context, screenTiming: String) {
        if (!isValidLocation(context, screenTiming)) return

        val wl = sScreenWakeLock
        if (wl != null && wl.isHeld) {
            wl.release()
            if (!wl.isHeld) {
                sScreenWakeLock = null
            }
        }
    }
}
