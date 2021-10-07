package xyz.aprildown.timer.domain.utils

import android.content.Context
import xyz.aprildown.timer.domain.R

object Constants {
    private const val EXTRA_PREFIX = "EXTRA_"
    const val EXTRA_TIMER_ID = "${EXTRA_PREFIX}ID"

    // Same as MachineService
    private const val ACTION_PREFIX = "COMMAND"
    const val ACTION_START = "${ACTION_PREFIX}_START"

    private const val CHANNEL_ID_PREFIX = "CHANNEL_"
    const val CHANNEL_SERVICE = "${CHANNEL_ID_PREFIX}SERVICE"
    const val CHANNEL_TIMING = "${CHANNEL_ID_PREFIX}TIMING"
    const val CHANNEL_SCREEN = "${CHANNEL_ID_PREFIX}SCREEN"
    const val CHANNEL_B_NOTIF = "${CHANNEL_ID_PREFIX}B_NOTIF"
    const val CHANNEL_APP_INFO_NOTIFICATION = "${CHANNEL_ID_PREFIX}APP_INFO_NOTIFICATION"

    const val NOTIF_ID_SERVICE = Int.MAX_VALUE
    const val NOTIF_ID_SCREEN = Int.MAX_VALUE - 1
    const val NOTIF_ID_NOTIFICATION = Int.MAX_VALUE - 2
    const val NOTIF_ID_APP_INFO = Int.MAX_VALUE - 3

    // region Links

    private const val LINK_PREFIX = "https://github.com/DeweyReed/Grocery/blob/master"
    private const val SUFFIX_CONTENT = "#readme"
    private val Context.languageTag
        get() = when (val tag = getString(R.string.language)) {
            "zh-rCN" -> tag
            else -> "en"
        }

    fun getPrivacyPolicyLink(): String {
        return "$LINK_PREFIX/tm-pp.md$SUFFIX_CONTENT"
    }

    fun getTermsOfServiceLink(): String {
        return "$LINK_PREFIX/tm-tos.md$SUFFIX_CONTENT"
    }

    fun getTipsAndTricksLink(context: Context): String {
        return "$LINK_PREFIX/${context.languageTag}/tips-and-tricks.md$SUFFIX_CONTENT"
    }

    fun getQaLink(context: Context): String {
        return "$LINK_PREFIX/${context.languageTag}/qa.md$SUFFIX_CONTENT"
    }

    fun getConfigureTtsLink(context: Context): String {
        return "$LINK_PREFIX/${context.languageTag}/configure-tts.md$SUFFIX_CONTENT"
    }

    fun getChangeLogLink(context: Context): String {
        return "$LINK_PREFIX/${context.languageTag}/change-log.md$SUFFIX_CONTENT"
    }

    // endregion Links

    const val FILENAME_RUNNING_LOG = "running.log"

    const val REMINDER_RATE = "rate"

    const val PREF_HAS_ANY_PURCHASE = "pref_has_subscription" // For backward compatibility
    const val PREF_HAS_PRO = "pref_has_pro"
    const val PREF_HAS_BACKUP_SUB = "pref_has_backup_sub"

    const val PREF_HAS_RUNNING_TIMERS = "has_running_timers"
}
