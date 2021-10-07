package xyz.aprildown.timer.domain.utils

import xyz.aprildown.timer.domain.BuildConfig

object AppConfig {
    val showFirstTimeInfo: Boolean = BuildConfig.SHOW_FIRST_TIME
    val openDebug: Boolean = BuildConfig.OPEN_DEBUG
}
