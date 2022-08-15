package xyz.aprildown.timer.app.base.data

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.Uri
import android.text.format.DateUtils
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.core.net.toUri
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.timer.app.base.utils.ScreenWakeLock
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.tools.helper.color
import xyz.aprildown.tools.helper.getNonNullString
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.tools.helper.toBoolean
import java.time.DayOfWeek
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY
import com.mikepenz.materialize.R as RMaterialize

object PreferenceData {

    const val KEY_APP_LANGUAGE = "key_app_language"
    // const val APP_LANGUAGE_DEFAULT = "default"
    // val Context.appLanguageTag: String
    //     get() = safeSharedPreference.getString(KEY_APP_LANGUAGE, APP_LANGUAGE_DEFAULT)

    /**
     * This key and its value will be used in the [ScreenWakeLock].
     */
    const val KEY_SCREEN = "key_screen"

    const val KEY_SCREEN_TIMING = "key_screen_timing"

    // region Tweak Time

    class TweakTimeSettings(context: Context) {

        val mainTime: Long
        val secondTime: Long
        val slots: List<Long>

        val hasSecond: Boolean get() = secondTime != 0L
        val hasSlot: Boolean get() = slots.any { it != 0L }

        init {
            val sp = context.safeSharedPreference

            if (!sp.contains(KEY_FIRST)) {
                sp.edit { putLong(KEY_FIRST, 60_000L) }
            }

            mainTime = sp.getLong(KEY_FIRST, 0L)
            secondTime = sp.getLong(KEY_SECOND, 0L)

            slots = List(4) {
                sp.getLong(getKey(it), 0L)
            }
        }

        companion object {
            private const val PREF_TWEAK_TIME_PREFIX = "tweak_time_time_"

            const val KEY_FIRST = "${PREF_TWEAK_TIME_PREFIX}0"
            const val KEY_SECOND = "${PREF_TWEAK_TIME_PREFIX}ni"
            fun getKey(index: Int): String = "$PREF_TWEAK_TIME_PREFIX${index + 1}"

            fun saveNewSettings(
                context: Context,
                mainTime: Long,
                secondTime: Long,
                slots: List<Long>
            ) {
                context.safeSharedPreference.edit {
                    putLong(KEY_FIRST, mainTime)
                    putLong(KEY_SECOND, secondTime)
                    slots.forEachIndexed { index, long -> putLong(getKey(index), long) }
                }
            }
        }
    }

    // endregion Tweak Time

    const val KEY_NOTIFIER_PLUS = "key_notifier_plus"
    val Context.shouldNotifierPlusGoBack: Boolean
        get() = safeSharedPreference.getString(KEY_NOTIFIER_PLUS, "1")
            ?.toIntOrNull()?.toBoolean() ?: true

    // region Phone Call

    const val KEY_PHONE_CALL = "key_phone_call"
    val Context.shouldPausePhoneCall: Boolean
        get() = safeSharedPreference.getNonNullString(KEY_PHONE_CALL, "2")
            .let { it == "1" || it == "2" }
    val Context.shouldResumePhoneCall: Boolean
        get() = safeSharedPreference.getNonNullString(KEY_PHONE_CALL, "2").let { it == "2" }

    fun SharedPreferences.disablePhoneCallBehavior() {
        edit { putString(KEY_PHONE_CALL, "0") }
    }

    // endregion Phone Call

    // region Show Timer Total Time

    const val KEY_SHOW_TIMER_TOTAL_TIME = "key_show_timer_total_time"
    var Context.showTimerTotalTime: Boolean
        get() = safeSharedPreference.getBoolean(KEY_SHOW_TIMER_TOTAL_TIME, false)
        set(value) = safeSharedPreference.edit { putBoolean(KEY_SHOW_TIMER_TOTAL_TIME, value) }

    // endregion Show Timer Total Time

    // region Week Start

    /**
     *  <item>2</item> <!-- Calendar.MONDAY -->
     *  <item>7</item> <!-- Calendar.SATURDAY -->
     *  <item>1</item> <!-- Calendar.SUNDAY -->
     */
    const val KEY_WEEK_START = "key_week_start"
    val Context.startWeekOn: Int
        get() = safeSharedPreference.getNonNullString(KEY_WEEK_START, "2").toIntOrNull() ?: 2

    val Context.startDayOfWeek: DayOfWeek
        get() = when (startWeekOn) {
            MONDAY -> DayOfWeek.MONDAY
            TUESDAY -> DayOfWeek.TUESDAY
            WEDNESDAY -> DayOfWeek.WEDNESDAY
            THURSDAY -> DayOfWeek.THURSDAY
            FRIDAY -> DayOfWeek.FRIDAY
            SATURDAY -> DayOfWeek.SATURDAY
            SUNDAY -> DayOfWeek.SUNDAY
            else -> throw IllegalStateException("Unknown weekday $startWeekOn")
        }

    // endregion Week Start

    const val KEY_MEDIA_STYLE_NOTIFICATION = "key_media_style_notification"
    val SharedPreferences.useMediaStyleNotification: Boolean
        get() = getBoolean(KEY_MEDIA_STYLE_NOTIFICATION, false)

    const val KEY_AUDIO_FOCUS_TYPE = "key_audio_focus_key"
    val Context.storedAudioFocusType: Int
        get() = safeSharedPreference.getNonNullString(
            KEY_AUDIO_FOCUS_TYPE,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT.toString()
        ).toInt()

    const val KEY_AUDIO_TYPE = "key_audio_key"
    val Context.storedAudioTypeValue: Int
        get() = safeSharedPreference.getNonNullString(
            KEY_AUDIO_TYPE,
            AudioManager.STREAM_MUSIC.toString()
        ).toInt()

    // region Labs

    private const val KEY_LABS = "key_labs"

    // Only used to remove them from SharedPreferences
    const val KEY_LABS_HALF_COUNT = "${KEY_LABS}_half_count"
    const val KEY_LABS_NOTIFICATION = "${KEY_LABS}_notification"
    const val KEY_LABS_TIME_PANEL = "${KEY_LABS}_time_panel"

    // endregion Labs

    // region One Layout

    const val KEY_ONE_LAYOUT = "key_one_layout_fix"
    var Context.oneLayout: String
        get() = safeSharedPreference.getNonNullString(KEY_ONE_LAYOUT, "one")
        set(value) {
            safeSharedPreference.edit {
                putString(KEY_ONE_LAYOUT, value)
            }
        }

    // region One Layout: One

    const val PREF_ONE_LAYOUT_ONE_TIMING_BAR = "pref_one_one_timing_bar"
    var Context.oneOneUsingTimingBar: Boolean
        get() = safeSharedPreference.getBoolean(PREF_ONE_LAYOUT_ONE_TIMING_BAR, false)
        set(value) = safeSharedPreference.edit { putBoolean(PREF_ONE_LAYOUT_ONE_TIMING_BAR, value) }

    const val PREF_ONE_LAYOUT_ONE_TIME_SIZE = "pref_one_one_time_text_size"
    var Context.oneOneTimeSize: Int
        get() = safeSharedPreference.getInt(PREF_ONE_LAYOUT_ONE_TIME_SIZE, 36)
        set(value) = safeSharedPreference.edit { putInt(PREF_ONE_LAYOUT_ONE_TIME_SIZE, value) }

    const val PREF_ONE_LAYOUT_ONE_ACTIONS = "pref_one_one_four_actions"
    const val ONE_LAYOUT_ONE_ACTION_STOP = "stop"
    const val ONE_LAYOUT_ONE_ACTION_PREV = "prev"
    const val ONE_LAYOUT_ONE_ACTION_NEXT = "next"
    const val ONE_LAYOUT_ONE_ACTION_MORE = "more"
    const val ONE_LAYOUT_ONE_ACTION_LOCK = "lock"
    const val ONE_LAYOUT_ONE_ACTION_EDIT = "edit"
    var Context.oneOneFourActions: List<String>
        get() {
            val sp = safeSharedPreference
            return if (sp.contains(PREF_ONE_LAYOUT_ONE_ACTIONS)) {
                sp.getNonNullString(PREF_ONE_LAYOUT_ONE_ACTIONS, "").split(",")
            } else {
                listOf(
                    ONE_LAYOUT_ONE_ACTION_STOP,
                    ONE_LAYOUT_ONE_ACTION_PREV,
                    ONE_LAYOUT_ONE_ACTION_NEXT,
                    ONE_LAYOUT_ONE_ACTION_MORE
                )
            }
        }
        set(value) {
            safeSharedPreference.edit {
                putString(PREF_ONE_LAYOUT_ONE_ACTIONS, value.joinToString(separator = ","))
            }
        }

    // endregion One Layout: One

    // endregion One Layout

    // region Time Panel

    enum class TimePanel(
        @DrawableRes val iconRes: Int,
        @StringRes val despRes: Int,
        @ColorRes val colorRes: Int
    ) {
        ELAPSED_TIME(
            iconRes = R.drawable.ic_time_panel_elapsed,
            despRes = R.string.time_panel_elapsed_time,
            colorRes = RMaterialize.color.md_grey_500
        ),
        ELAPSED_PERCENT(
            iconRes = R.drawable.ic_time_panel_elapsed,
            despRes = R.string.time_panel_elapsed_percent,
            colorRes = RMaterialize.color.md_grey_500
        ),
        REMAINING_TIME(
            iconRes = R.drawable.ic_time_panel_remaining,
            despRes = R.string.time_panel_remaining_time,
            colorRes = RMaterialize.color.md_green_500
        ),
        REMAINING_PERCENT(
            iconRes = R.drawable.ic_time_panel_remaining,
            despRes = R.string.time_panel_remaining_percent,
            colorRes = RMaterialize.color.md_green_500
        ),
        STEP_END_TIME(
            iconRes = R.drawable.ic_time_panel_step_end_time,
            despRes = R.string.time_panel_step_end_time,
            colorRes = RMaterialize.color.md_amber_800
        ),
        TIMER_END_TIME(
            iconRes = R.drawable.ic_time_panel_timer_end_time,
            despRes = R.string.time_panel_timer_end_time,
            colorRes = RMaterialize.color.md_amber_800
        );

        fun formatText(context: Context, data: Double): String = when (this) {
            ELAPSED_TIME, REMAINING_TIME -> data.toLong().produceTime()
            ELAPSED_PERCENT, REMAINING_PERCENT -> "${"%.1f".format(data)}%"
            STEP_END_TIME, TIMER_END_TIME ->
                DateUtils.formatDateTime(context, data.toLong(), DateUtils.FORMAT_SHOW_TIME)
        }
    }

    const val TIME_PANELS = "time_panels"
    var Context.timePanels: List<TimePanel>
        get() = safeSharedPreference.getString(TIME_PANELS, null)?.let { content ->
            val values = TimePanel.values()
            if (content.contains(",")) {
                content.split(",").map { str -> values[str.toInt()] }
            } else null
        } ?: emptyList()
        set(value) {
            safeSharedPreference.edit {
                putString(
                    TIME_PANELS,
                    value.joinToString(separator = ",") { it.ordinal.toString() }
                )
            }
        }

    // endregion Time Panel

    // region App Theme

    data class AppTheme(
        @ColorInt
        val colorPrimary: Int,
        @ColorInt
        val colorSecondary: Int,
        val sameStatusBar: Boolean = false,
        val enableNav: Boolean = false
    ) {
        companion object {
            private const val PREF_PREFIX = "pref_app_theme_"
            const val PREF_PRIMARY = "${PREF_PREFIX}primary"
            const val PREF_SECONDARY = "${PREF_PREFIX}accent"
            const val PREF_SAME_STATUS_BAR = "${PREF_PREFIX}same_status_bar"
            const val PREF_ENABLE_NAV = "${PREF_PREFIX}enable_nav"
        }
    }

    var Context.appTheme: AppTheme
        get() {
            val sp = safeSharedPreference
            return AppTheme(
                sp.getInt(AppTheme.PREF_PRIMARY, color(R.color.colorPrimary)),
                sp.getInt(AppTheme.PREF_SECONDARY, color(R.color.colorSecondary)),
                sp.getBoolean(AppTheme.PREF_SAME_STATUS_BAR, true),
                sp.getBoolean(AppTheme.PREF_ENABLE_NAV, false)
            )
        }
        set(value) {
            safeSharedPreference.edit {
                val (p, a, s, n) = value
                putInt(AppTheme.PREF_PRIMARY, p)
                putInt(AppTheme.PREF_SECONDARY, a)
                putBoolean(AppTheme.PREF_SAME_STATUS_BAR, s)
                putBoolean(AppTheme.PREF_ENABLE_NAV, n)
            }
        }

    // endregion App Theme

    // region Step Color

    const val KEY_STEP_NORMAL = "pref_step_color_normal"
    const val KEY_STEP_NOTIFIER = "pref_step_color_notifier"
    const val KEY_STEP_START = "pref_step_color_start"
    const val KEY_STEP_END = "pref_step_color_end"

    @ColorInt
    fun StepType.getTypeColor(context: Context): Int = when (this) {
        StepType.NORMAL -> context.safeSharedPreference.getInt(
            KEY_STEP_NORMAL,
            context.color(RMaterialize.color.md_purple_500)
        )
        StepType.NOTIFIER -> context.safeSharedPreference.getInt(
            KEY_STEP_NOTIFIER,
            context.color(RMaterialize.color.md_light_blue_500)
        )
        StepType.START -> context.safeSharedPreference.getInt(
            KEY_STEP_START,
            context.color(android.R.color.holo_green_dark)
        )
        StepType.END -> context.safeSharedPreference.getInt(
            KEY_STEP_END,
            context.color(android.R.color.holo_red_light)
        )
    }

    fun StepType.saveTypeColor(context: Context, @ColorInt color: Int) {
        context.safeSharedPreference.edit {
            putInt(
                when (this@saveTypeColor) {
                    StepType.NORMAL -> KEY_STEP_NORMAL
                    StepType.NOTIFIER -> KEY_STEP_NOTIFIER
                    StepType.START -> KEY_STEP_START
                    StepType.END -> KEY_STEP_END
                },
                color
            )
        }
    }

    // endregion StepColor

    private const val PREF_LAST_BACKUP_URI = "pref_last_backup_uri"
    var Context.lastBackupUri: Uri
        get() = safeSharedPreference.getNonNullString(PREF_LAST_BACKUP_URI, "").toUri()
        set(value) = safeSharedPreference.edit { putString(PREF_LAST_BACKUP_URI, value.toString()) }

    const val PREF_GRID_TIMER_LIST = "pref_grid_timer_list"
    var SharedPreferences.showGridTimerList: Boolean
        get() = getBoolean(PREF_GRID_TIMER_LIST, false)
        set(value) = edit { putBoolean(PREF_GRID_TIMER_LIST, value) }

    const val PREF_BAKED_COUNT = "pref_baked_count"
    var SharedPreferences.useBakedCount: Boolean
        get() = getBoolean(PREF_BAKED_COUNT, false)
        set(value) = edit { putBoolean(PREF_BAKED_COUNT, value) }
    const val BAKED_COUNT_NAME = "baked_count"

    private const val PREF_USE_VOICE_CONTENT2 = "pref_use_voice_content2"
    var SharedPreferences.useVoiceContent2: Boolean
        get() = getBoolean(PREF_USE_VOICE_CONTENT2, false)
        set(value) = edit { putBoolean(PREF_USE_VOICE_CONTENT2, value) }
}
