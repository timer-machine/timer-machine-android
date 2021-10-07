package xyz.aprildown.timer.app.base.utils

import android.content.Context
import android.os.PowerManager
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.tools.helper.safeSharedPreference

object ScreenWakeLock {
    private const val LOG_TAG = "TimeR Machine: Screen WakeLock"

    private var sScreenWakeLock: PowerManager.WakeLock? = null

    fun acquireScreenWakeLock(context: Context, screenTiming: String) {
        if (!isValidLocation(context, screenTiming)) return

        fun getPowerManager(): PowerManager {
            return context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        }

        val wl = when (
            context.safeSharedPreference.getString(
                PreferenceData.KEY_SCREEN,
                context.getString(R.string.pref_screen_value_default)
            )
        ) {
            context.getString(R.string.pref_screen_value_keep) -> {
                getPowerManager().newWakeLock(
                    @Suppress("DEPRECATION") PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    LOG_TAG
                )
            }
            context.getString(R.string.pref_screen_value_dim) -> {
                getPowerManager().newWakeLock(
                    @Suppress("DEPRECATION") PowerManager.SCREEN_DIM_WAKE_LOCK,
                    LOG_TAG
                )
            }
            else -> return
        }
        sScreenWakeLock = wl
        wl.setReferenceCounted(false)
        @Suppress("WakelockTimeout")
        wl.acquire()
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
        }
        sScreenWakeLock = null
    }
}
