package xyz.aprildown.timer.app.backup

import android.content.Context
import android.content.SharedPreferences
import com.github.deweyreed.tools.helper.getNonNullString
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.aprildown.timer.app.base.data.DarkTheme
import xyz.aprildown.timer.app.base.data.FloatingWindowPip
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.data.PreferenceData.appTheme
import xyz.aprildown.timer.app.base.data.PreferenceData.getTypeColor
import xyz.aprildown.timer.app.base.data.PreferenceData.oneLayout
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneFourActions
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneTimeSize
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneUsingTimingBar
import xyz.aprildown.timer.app.base.data.PreferenceData.saveTypeColor
import xyz.aprildown.timer.app.base.data.PreferenceData.shouldNotifierPlusGoBack
import xyz.aprildown.timer.app.base.data.PreferenceData.showTimerTotalTime
import xyz.aprildown.timer.app.base.data.PreferenceData.startWeekOn
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioFocusType
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue
import xyz.aprildown.timer.app.base.data.PreferenceData.useMediaStyleNotification
import xyz.aprildown.timer.app.base.utils.AppThemeUtils
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.repositories.AppPreferencesProvider
import xyz.aprildown.timer.domain.usecases.folder.FolderSortByRule
import xyz.aprildown.tools.helper.safeSharedPreference
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

private fun Map<String, String>.ifHasKey(key: String, f: (String) -> Unit) {
    get(key)?.let(f)
}

private interface PreferenceItem {
    fun storeToMap(context: Context, map: MutableMap<String, String>)
    fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    )

    companion object {
        fun getPreferenceItems(): List<PreferenceItem> {
            return listOf(
                DarkThemePreferenceItem(),
                ScreenPreferenceItem(),
                TweakTimeItem(),
                NotifierPlusItem(),
                FloatingWindowPipItem(),
                PhoneCallItem(),
                WeekStartPreferenceItem(),
                AudioTypePreferenceItem(),
                OneLayoutPreferenceItem(),
                ThemePreferenceItem(),
                ShowTimerTotalTimePreferenceItem(),
                FolderSortByItem(),
                GridTimerListItem(),
                TtsBakeryListItem(),
                MediaStyleNotificationItem(),
            )
        }
    }
}

@Reusable
class AppPreferencesProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences
) : AppPreferencesProvider {
    override fun getAppPreferences(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        PreferenceItem.getPreferenceItems().forEach { item ->
            item.storeToMap(context, result)
        }
        return result
    }

    override fun applyAppPreferences(prefs: Map<String, String>) {
        val editor = sharedPreferences.edit()
        PreferenceItem.getPreferenceItems().forEach { item ->
            item.storeToApp(context, prefs, editor)
        }
        editor.apply()
    }
}

private class DarkThemePreferenceItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        val darkTheme = DarkTheme(context)
        map[DarkTheme.PREF_DARK_THEME] = darkTheme.darkThemeValue.toString()
        map[DarkTheme.PREF_DARK_THEME_SCHEDULED_ENABLE] = darkTheme.scheduleEnabled.toString()
        map[DarkTheme.PREF_DARK_THEME_SCHEDULED_RANGE] = darkTheme.scheduleRange.toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        val darkTheme = DarkTheme(context)
        prefs.ifHasKey(DarkTheme.PREF_DARK_THEME) {
            darkTheme.darkThemeValue = it.toInt()
        }
        prefs.ifHasKey(DarkTheme.PREF_DARK_THEME_SCHEDULED_ENABLE) {
            darkTheme.scheduleEnabled = it.toBoolean()
        }
        prefs.ifHasKey(DarkTheme.PREF_DARK_THEME_SCHEDULED_RANGE) {
            darkTheme.scheduleRange = DarkTheme.ScheduledRange.fromString(it)
        }
    }
}

private class ScreenPreferenceItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        val sharedPreferences = context.safeSharedPreference
        map[KEY] = sharedPreferences.getNonNullString(
            KEY,
            context.getString(RBase.string.pref_screen_value_default)
        )
        map[KEY_TIMING] = sharedPreferences.getNonNullString(
            KEY_TIMING,
            context.getString(RBase.string.pref_screen_timing_value_default)
        )
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) { editor.putString(KEY, it) }
        prefs.ifHasKey(KEY_TIMING) { editor.putString(KEY_TIMING, it) }
    }

    companion object {
        private const val KEY = PreferenceData.KEY_SCREEN
        private const val KEY_TIMING = PreferenceData.KEY_SCREEN_TIMING
    }
}

private class TweakTimeItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        val settings = PreferenceData.TweakTimeSettings(context)
        map[KEY] = settings.mainTime.toString()
        if (settings.hasSecond) {
            map[PreferenceData.TweakTimeSettings.KEY_SECOND] = settings.secondTime.toString()
        }
        if (settings.hasSlot) {
            settings.slots.forEachIndexed { i, amount ->
                map[PreferenceData.TweakTimeSettings.getKey(i)] = amount.toString()
            }
        }
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) { mainTime ->
            PreferenceData.TweakTimeSettings.saveNewSettings(
                context,
                mainTime.toLong(),
                prefs[PreferenceData.TweakTimeSettings.KEY_SECOND]?.toLong() ?: 0L,
                List(4) {
                    val key = PreferenceData.TweakTimeSettings.getKey(it)
                    prefs[key]?.toLongOrNull() ?: 0L
                }
            )
        }
    }

    companion object {
        private const val KEY = PreferenceData.TweakTimeSettings.KEY_FIRST
    }
}

private class NotifierPlusItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY] = context.shouldNotifierPlusGoBack.toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) { editor.putString(KEY, it) }
    }

    companion object {
        private const val KEY = PreferenceData.KEY_NOTIFIER_PLUS
    }
}

private class FloatingWindowPipItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        val floatingWindowPip = FloatingWindowPip(context)
        map[FloatingWindowPip.KEY_AUTO_CLOSE] = floatingWindowPip.autoClose.toString()
        map[FloatingWindowPip.KEY_FLOATING_WINDOW_ALPHA] =
            floatingWindowPip.floatingWindowAlpha.toString()
        map[FloatingWindowPip.KEY_FLOATING_WINDOW_SIZE] =
            floatingWindowPip.floatingWindowSize.toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        val floatingWindowPip = FloatingWindowPip(context)
        prefs.ifHasKey(FloatingWindowPip.KEY_AUTO_CLOSE) {
            floatingWindowPip.autoClose = it.toBoolean()
        }
        prefs.ifHasKey(FloatingWindowPip.KEY_FLOATING_WINDOW_ALPHA) {
            floatingWindowPip.floatingWindowAlpha = it.toFloat()
        }
        prefs.ifHasKey(FloatingWindowPip.KEY_FLOATING_WINDOW_SIZE) {
            floatingWindowPip.floatingWindowSize = it.toFloat()
        }
    }
}

private class PhoneCallItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY] = context.safeSharedPreference.getNonNullString(KEY, "2")
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) { editor.putString(KEY, it) }
    }

    companion object {
        private const val KEY = PreferenceData.KEY_PHONE_CALL
    }
}

private class WeekStartPreferenceItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY] = context.startWeekOn.toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) { editor.putString(KEY, it) }
    }

    companion object {
        private const val KEY = PreferenceData.KEY_WEEK_START
    }
}

private class AudioTypePreferenceItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY_AUDIO] = context.storedAudioTypeValue.toString()
        map[KEY_FOCUS] = context.storedAudioFocusType.toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY_AUDIO) { editor.putString(KEY_AUDIO, it) }
        prefs.ifHasKey(KEY_FOCUS) { editor.putString(KEY_FOCUS, it) }
    }

    companion object {
        private const val KEY_AUDIO = PreferenceData.KEY_AUDIO_TYPE
        private const val KEY_FOCUS = PreferenceData.KEY_AUDIO_FOCUS_TYPE
    }
}

private class OneLayoutPreferenceItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY_LAYOUT] = context.oneLayout
        map[KEY_ONE_BAR] = context.oneOneUsingTimingBar.toString()
        map[KEY_ONE_SIZE] = context.oneOneTimeSize.toString()
        map[KEY_FOUR_ACTIONS] = context.oneOneFourActions.joinToString(separator = ",")
        map[KEY_TIME_PANELS] = context.safeSharedPreference.getNonNullString(KEY_TIME_PANELS, "")
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY_LAYOUT) {
            editor.putString(KEY_LAYOUT, it)
        }
        prefs.ifHasKey(KEY_ONE_BAR) {
            context.oneOneUsingTimingBar = it.toBoolean()
        }
        prefs.ifHasKey(KEY_ONE_SIZE) {
            context.oneOneTimeSize = it.toInt()
        }
        prefs.ifHasKey(KEY_FOUR_ACTIONS) {
            context.oneOneFourActions = it.split(",")
        }
        prefs.ifHasKey(KEY_TIME_PANELS) {
            editor.putString(KEY_TIME_PANELS, it)
        }
    }

    companion object {
        private const val KEY_LAYOUT = PreferenceData.KEY_ONE_LAYOUT
        private const val KEY_ONE_BAR = PreferenceData.PREF_ONE_LAYOUT_ONE_TIMING_BAR
        private const val KEY_ONE_SIZE = PreferenceData.PREF_ONE_LAYOUT_ONE_TIME_SIZE
        private const val KEY_FOUR_ACTIONS = PreferenceData.PREF_ONE_LAYOUT_ONE_ACTIONS
        private const val KEY_TIME_PANELS = PreferenceData.TIME_PANELS
    }
}

private class ThemePreferenceItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        val appTheme = context.appTheme

        map[PreferenceData.AppTheme.PREF_TYPE] = appTheme.type.toString()
        map[PreferenceData.AppTheme.PREF_PRIMARY] = appTheme.colorPrimary.toString()
        map[PreferenceData.AppTheme.PREF_SECONDARY] = appTheme.colorSecondary.toString()
        map[PreferenceData.AppTheme.PREF_SAME_STATUS_BAR] = appTheme.sameStatusBar.toString()
        map[PreferenceData.AppTheme.PREF_ENABLE_NAV] = appTheme.enableNav.toString()

        map[PreferenceData.KEY_STEP_NORMAL] = StepType.NORMAL.getTypeColor(context).toString()
        map[PreferenceData.KEY_STEP_NOTIFIER] = StepType.NOTIFIER.getTypeColor(context).toString()
        map[PreferenceData.KEY_STEP_START] = StepType.START.getTypeColor(context).toString()
        map[PreferenceData.KEY_STEP_END] = StepType.END.getTypeColor(context).toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(PreferenceData.AppTheme.PREF_PRIMARY) { primary ->
            prefs.ifHasKey(PreferenceData.AppTheme.PREF_SECONDARY) { secondary ->
                prefs.ifHasKey(PreferenceData.AppTheme.PREF_SAME_STATUS_BAR) { sameStatus ->
                    prefs.ifHasKey(PreferenceData.AppTheme.PREF_ENABLE_NAV) { enableNav ->
                        PreferenceData.AppTheme(
                            type = prefs.getOrDefault(
                                PreferenceData.AppTheme.PREF_TYPE,
                                PreferenceData.AppTheme.AppThemeType.TYPE_COLOR.toString()
                            ).toInt(),
                            colorPrimary = primary.toInt(),
                            colorSecondary = secondary.toInt(),
                            sameStatusBar = sameStatus.toBoolean(),
                            enableNav = enableNav.toBoolean(),
                        ).also {
                            context.appTheme = it
                            AppThemeUtils.configAppTheme(context, it)
                        }

                        prefs.ifHasKey(PreferenceData.KEY_STEP_NORMAL) {
                            StepType.NORMAL.saveTypeColor(context, it.toInt())
                        }
                        prefs.ifHasKey(PreferenceData.KEY_STEP_NOTIFIER) {
                            StepType.NOTIFIER.saveTypeColor(context, it.toInt())
                        }
                        prefs.ifHasKey(PreferenceData.KEY_STEP_START) {
                            StepType.START.saveTypeColor(context, it.toInt())
                        }
                        prefs.ifHasKey(PreferenceData.KEY_STEP_END) {
                            StepType.END.saveTypeColor(context, it.toInt())
                        }
                    }
                }
            }
        }
    }
}

private class ShowTimerTotalTimePreferenceItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY] = context.showTimerTotalTime.toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) {
            context.showTimerTotalTime = it.toBoolean()
        }
    }

    companion object {
        private const val KEY = PreferenceData.KEY_SHOW_TIMER_TOTAL_TIME
    }
}

private class FolderSortByItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY] =
            context.safeSharedPreference.getInt(KEY, FolderSortBy.AddedOldest.ordinal).toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) {
            editor.putInt(KEY, it.toInt())
        }
    }

    companion object {
        private const val KEY = FolderSortByRule.PREF_FOLDER_SORT_BY
    }
}

private class GridTimerListItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY] = context.safeSharedPreference.getBoolean(KEY, false).toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) {
            editor.putBoolean(KEY, it.toBoolean())
        }
    }

    companion object {
        private const val KEY = PreferenceData.PREF_GRID_TIMER_LIST
    }
}

private class TtsBakeryListItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY] = context.safeSharedPreference.getBoolean(KEY, false).toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) {
            editor.putBoolean(KEY, it.toBoolean())
        }
    }

    companion object {
        private const val KEY = PreferenceData.PREF_IS_TTS_BAKERY_OPEN
    }
}

private class MediaStyleNotificationItem : PreferenceItem {
    override fun storeToMap(context: Context, map: MutableMap<String, String>) {
        map[KEY] = context.safeSharedPreference.useMediaStyleNotification.toString()
    }

    override fun storeToApp(
        context: Context,
        prefs: Map<String, String>,
        editor: SharedPreferences.Editor
    ) {
        prefs.ifHasKey(KEY) {
            editor.putBoolean(KEY, it.toBoolean())
        }
    }

    companion object {
        private const val KEY = PreferenceData.KEY_MEDIA_STYLE_NOTIFICATION
    }
}
