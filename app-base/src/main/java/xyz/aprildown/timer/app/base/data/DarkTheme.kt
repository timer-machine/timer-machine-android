package xyz.aprildown.timer.app.base.data

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import xyz.aprildown.timer.domain.utils.AppConfig
import xyz.aprildown.tools.helper.getNonNullString
import xyz.aprildown.tools.helper.safeSharedPreference
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DarkTheme(context: Context) {
    private val sp = context.safeSharedPreference

    var darkThemeValue: Int
        get() {
            if (!sp.contains(PREF_DARK_THEME)) {
                sp.edit { putInt(PREF_DARK_THEME, calculateDefaultValue()) }
            }
            return sp.getInt(PREF_DARK_THEME, 0)
        }
        set(value) = sp.edit { putInt(PREF_DARK_THEME, value) }

    var manualOn: Boolean
        get() = sp.getBoolean(PREF_DARK_THEME_MANUAL_ON, false)
        set(value) = sp.edit { putBoolean(PREF_DARK_THEME_MANUAL_ON, value) }

    var scheduleEnabled: Boolean
        get() = sp.getBoolean(PREF_DARK_THEME_SCHEDULED_ENABLE, false)
        set(value) = sp.edit { putBoolean(PREF_DARK_THEME_SCHEDULED_ENABLE, value) }

    var scheduleRange: ScheduledRange
        get() = ScheduledRange.fromString(
            sp.getNonNullString(
                PREF_DARK_THEME_SCHEDULED_RANGE,
                "22,0,6,0"
            )
        )
        set(value) = sp.edit { putString(PREF_DARK_THEME_SCHEDULED_RANGE, value.toString()) }

    fun applyAppCompatDelegate() {
        AppCompatDelegate.setDefaultNightMode(
            when (darkThemeValue) {
                DARK_THEME_MANUAL -> {
                    if (manualOn) {
                        AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        AppCompatDelegate.MODE_NIGHT_NO
                    }
                }
                DARK_THEME_SYSTEM_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                DARK_THEME_BATTERY_SAVER -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
            }
        )
    }

    /**
     * @return True if we should revert the current dark.
     */
    fun calculateAutoDarkChange(
        currentIsDark: Boolean,
        nowMilli: Long,
        lastLaunchMilli: Long
    ): Boolean {
        val lastLaunch =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(lastLaunchMilli), ZoneId.systemDefault())
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMilli), ZoneId.systemDefault())
        val today = LocalDate.now()
        val nightRange = scheduleRange
        val nightStart =
            LocalDateTime.of(today, LocalTime.of(nightRange.fromHour, nightRange.fromMinute))
        val nightEnd =
            LocalDateTime.of(today, LocalTime.of(nightRange.toHour, nightRange.toMinute))

        fun isEnteringTheSpanFirst(from: LocalDateTime, to: LocalDateTime): Boolean {
            if (from == to) return false
            require(from.isBefore(to))
            return (now.isAfter(from) || now == from) &&
                now.isBefore(to) &&
                lastLaunch.isBefore(from)
        }

        fun isEnteringNight(nightStart: LocalDateTime, nightEnd: LocalDateTime): Boolean {
            return isEnteringTheSpanFirst(from = nightStart, to = nightEnd) && !currentIsDark
        }

        fun isExitingNight(nightEnd: LocalDateTime, nextNightStart: LocalDateTime): Boolean {
            return isEnteringTheSpanFirst(from = nightEnd, to = nextNightStart) && currentIsDark
        }

        return if (nightStart.isBefore(nightEnd)) {
            // |day start           |night start------------|night end          |day end
            when {
                now.isBefore(nightStart) -> {
                    isExitingNight(
                        nightEnd = nightEnd.minusDays(1),
                        nextNightStart = nightStart
                    )
                }
                now.isBefore(nightEnd) -> {
                    isEnteringNight(
                        nightStart = nightStart,
                        nightEnd = nightEnd
                    )
                }
                else -> {
                    isExitingNight(
                        nightEnd = nightEnd,
                        nextNightStart = nightStart.plusDays(1)
                    )
                }
            }
        } else {
            // |day start-----------|night end              |night start--------|day end
            when {
                now.isBefore(nightEnd) -> {
                    isEnteringNight(
                        nightStart = nightStart.minusDays(1),
                        nightEnd = nightEnd
                    )
                }
                now.isBefore(nightStart) -> {
                    isExitingNight(
                        nightEnd = nightEnd,
                        nextNightStart = nightStart
                    )
                }
                else -> {
                    isEnteringNight(
                        nightStart = nightStart,
                        nightEnd = nightEnd.plusDays(1)
                    )
                }
            }
        }
    }

    data class ScheduledRange(
        val fromHour: Int,
        val fromMinute: Int,
        val toHour: Int,
        val toMinute: Int
    ) {
        override fun toString(): String {
            return "$fromHour,$fromMinute,$toHour,$toMinute"
        }

        companion object {
            fun fromString(str: String): ScheduledRange {
                val span = str.split(',').map { it.toInt() }
                return ScheduledRange(span[0], span[1], span[2], span[3])
            }
        }
    }

    companion object {

        const val PREF_DARK_THEME = "pref_dark_theme"
        const val DARK_THEME_MANUAL = 1
        const val DARK_THEME_SYSTEM_DEFAULT = 2
        const val DARK_THEME_BATTERY_SAVER = 3

        const val PREF_DARK_THEME_MANUAL_ON = "pref_night_theme"

        const val PREF_DARK_THEME_SCHEDULED_ENABLE = "pref_auto_night_enabled"
        const val PREF_DARK_THEME_SCHEDULED_RANGE = "pref_auto_night_span"
    }
}

/**
 * Calculate dark theme value if the value is missing.
 */
private fun DarkTheme.calculateDefaultValue(): Int = when {
    AppConfig.openDebug || scheduleEnabled || manualOn -> DarkTheme.DARK_THEME_MANUAL
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> DarkTheme.DARK_THEME_SYSTEM_DEFAULT
    else -> DarkTheme.DARK_THEME_BATTERY_SAVER
}
