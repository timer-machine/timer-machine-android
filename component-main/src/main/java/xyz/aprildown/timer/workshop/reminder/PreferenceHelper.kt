package xyz.aprildown.timer.workshop.reminder

import android.content.Context
import java.util.Date

internal class PreferenceHelper(context: Context, id: String) {

    private val prefShouldShow = "${PREF_PREFIX}_${id}_should_show"
    private val prefInstallDate = "${PREF_PREFIX}_${id}_install_date"
    private val prefLaunchTimes = "${PREF_PREFIX}_${id}_launch_times"
    private val prefRemindInterval = "${PREF_PREFIX}_${id}_remind_interval"

    private val prefs = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

    var shouldShow: Boolean
        get() = prefs.getBoolean(prefShouldShow, true)
        set(value) = prefs.edit().putBoolean(prefShouldShow, value).apply()

    var installDate: Long
        get() = prefs.getLong(prefInstallDate, 0L)
        set(_) = prefs.edit().putLong(prefInstallDate, Date().time).apply()

    var launchTimes: Int
        get() = prefs.getInt(prefLaunchTimes, 0)
        set(value) = prefs.edit().putInt(prefLaunchTimes, value).apply()

    var remindInterval: Long
        get() = prefs.getLong(prefRemindInterval, 0L)
        set(_) = prefs.edit().putLong(prefRemindInterval, Date().time).apply()

    val isFirstLaunch: Boolean
        get() = installDate == 0L

    fun clear() {
        prefs.edit().remove(prefInstallDate).remove(prefLaunchTimes).apply()
    }
}

private const val PREF_PREFIX = "app_reminder"
private const val PREF_FILE_NAME = "${PREF_PREFIX}_pref_file"
