package xyz.aprildown.timer.app.base.utils

import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.github.deweyreed.tools.anko.dp
import com.github.deweyreed.tools.helper.color
import com.github.deweyreed.tools.utils.ThemeColorUtils
import com.google.android.material.elevation.ElevationOverlayProvider
import xyz.aprildown.theme.Theme
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.data.PreferenceData.appTheme
import com.google.android.material.R as RMaterial

object AppThemeUtils {

    // ColorRes -> ColorInt
    private val dynamicColorMap: ArrayMap<Int, Int> = arrayMapOf()

    fun configAppTheme(context: Context, appTheme: PreferenceData.AppTheme) {
        val primary = appTheme.colorPrimary
        val secondary = appTheme.colorSecondary
        val sameStatusBar = appTheme.sameStatusBar
        val enableNav = appTheme.enableNav
        Theme.edit(context) {
            colorPrimary = primary
            val darkerPrimary = darker(primary)
            colorPrimaryVariant = darkerPrimary
            colorOnPrimary = calculateOnColor(primary)
            colorSecondary = secondary
            colorSecondaryVariant = darker(secondary)
            colorOnSecondary = calculateOnColor(secondary)
            colorStatusBar = if (sameStatusBar) primary else darkerPrimary
            colorNavigationBar = if (enableNav) primary else null
            lightStatusByPrimary = true
        }

        fun registerColorRes(@ColorRes colorRes: Int) {
            dynamicColorMap[colorRes] = context.color(colorRes)
        }

        dynamicColorMap.clear()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                appTheme.type == PreferenceData.AppTheme.AppThemeType.TYPE_DYNAMIC_DARK -> {
                registerColorRes(PreferenceData.AppTheme.dynamicDarkPrimaryColorRes)
                registerColorRes(PreferenceData.AppTheme.dynamicDarkSecondaryColorRes)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                appTheme.type == PreferenceData.AppTheme.AppThemeType.TYPE_DYNAMIC_LIGHT -> {
                registerColorRes(PreferenceData.AppTheme.dynamicLightPrimaryColorRes)
                registerColorRes(PreferenceData.AppTheme.dynamicLightSecondaryColorRes)
            }
            else -> Unit
        }
    }

    /**
     * Since I can't find a way to know when the dynamic colors change, I have to check colors
     * manually in the [android.app.Application.onConfigurationChanged].
     */
    fun syncDynamicTheme(context: Context) {
        if (dynamicColorMap.any { context.color(it.key) != it.value }) {
            configAppTheme(context, context.appTheme)
        }
    }

    /**
     * I know pure black is better but I love #8A000000 so much.
     * [RMaterial.dimen.material_emphasis_medium]
     */
    fun calculateOnColor(@ColorInt color: Int): Int {
        return if (ThemeColorUtils.isLightColor(color)) {
            ThemeColorUtils.adjustAlpha(Color.BLACK, 0.6f)
        } else {
            Color.WHITE
        }
    }

    fun configThemeForDark(context: Context, isDark: Boolean) {
        val appTheme = context.appTheme
        val primary = appTheme.colorPrimary
        val sameStatusBar = appTheme.sameStatusBar
        val enableNav = appTheme.enableNav

        if (isDark) {
            Theme.edit(context) {
                val color = calculateToolbarColorDuringNight(context)
                colorStatusBar = color
                if (enableNav) {
                    colorNavigationBar = color
                }
                lightStatusByPrimary = false
            }
        } else {
            Theme.edit(context) {
                colorStatusBar = if (sameStatusBar) primary else darker(primary)
                if (enableNav) {
                    colorNavigationBar = primary
                }
                lightStatusByPrimary = true
            }
        }
    }

    @ColorInt
    fun calculateToolbarColorDuringNight(context: Context): Int {
        return ThemeColorUtils.adjustAlpha(
            Color.BLACK,
            // The app bar normally has an elevation of 4dp.
            ElevationOverlayProvider(context).calculateOverlayAlphaFraction(context.dp(4))
        )
    }
}
