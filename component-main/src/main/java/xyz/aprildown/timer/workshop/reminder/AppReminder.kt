package xyz.aprildown.timer.workshop.reminder

import android.content.Context
import java.util.Date

@Suppress("unused")
class AppReminder(context: Context, id: String) {

    private val pref = PreferenceHelper(context, id)

    private var installDate = 5
    private var launchTimes = 10
    private var remindInterval = 3

    private var isDebug = false

    private val isOverInstallDate: Boolean
        get() = isOverDate(pref.installDate, installDate)

    private val isOverLaunchTimes: Boolean
        get() = pref.launchTimes >= launchTimes

    private val isOverRemindDate: Boolean
        get() = isOverDate(pref.remindInterval, remindInterval)

    fun setLaunchTimes(launchTimes: Int): AppReminder {
        this.launchTimes = launchTimes
        return this
    }

    fun setInstallDays(installDate: Int): AppReminder {
        this.installDate = installDate
        return this
    }

    fun setRemindInterval(remindInterval: Int): AppReminder {
        this.remindInterval = remindInterval
        return this
    }

    fun getAgreeToShow(): Boolean = pref.shouldShow
    fun setAgreeToShow(clear: Boolean): AppReminder {
        pref.shouldShow = clear
        return this
    }

    fun clearSettingsParam(): AppReminder {
        pref.shouldShow = true
        pref.clear()
        return this
    }

    fun monitor(): AppReminder {
        if (pref.isFirstLaunch) {
            pref.installDate = 0
        }
        pref.launchTimes += 1
        return this
    }

    fun setDebug(isDebug: Boolean): AppReminder {
        this.isDebug = isDebug
        return this
    }

    fun ok() {
        pref.shouldShow = false
    }

    fun later() {
        pref.remindInterval = 0
    }

    fun no() {
        pref.shouldShow = false
    }

    fun ifConditionsAreMet(): Boolean = isDebug ||
        (pref.shouldShow && isOverLaunchTimes && isOverInstallDate && isOverRemindDate)

    inline fun ifConditionsAreMetThen(f: () -> Unit) {
        if (ifConditionsAreMet()) f.invoke()
    }
}

private fun isOverDate(targetDate: Long, threshold: Int): Boolean {
    return Date().time - targetDate >= threshold * 24 * 60 * 60 * 1000
}
