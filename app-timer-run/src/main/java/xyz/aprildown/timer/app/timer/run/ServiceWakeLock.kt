package xyz.aprildown.timer.app.timer.run

import android.content.Context
import android.os.PowerManager

internal object ServiceWakeLock {
    private const val LOG_TAG = "TimeR Machine: Timing WakeLock"

    private var cpuWakeLock: PowerManager.WakeLock? = null

    private fun createPartialWakeLock(context: Context): PowerManager.WakeLock {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOG_TAG)
    }

    fun acquireCpuWakeLock(context: Context) {
        if (cpuWakeLock != null) {
            return
        }
        val wl = createPartialWakeLock(context)
        cpuWakeLock = wl
        wl.setReferenceCounted(false)
        @Suppress("WakelockTimeout")
        wl.acquire()
    }

    fun releaseCpuLock() {
        val wl = cpuWakeLock
        if (wl != null && wl.isHeld) {
            wl.release()
        }
        cpuWakeLock = null
    }
}
