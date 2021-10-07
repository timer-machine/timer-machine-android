package xyz.aprildown.timer.app.base.utils

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import com.google.android.material.elevation.ElevationOverlayProvider
import xyz.aprildown.theme.Theme
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.data.PreferenceData.appTheme
import xyz.aprildown.tools.anko.dp
import xyz.aprildown.tools.utils.ThemeColorUtils

object AppThemeUtils {

    fun configAppTheme(context: Context, appTheme: PreferenceData.AppTheme) {
        val (primary, secondary, sameStatusBar, enableNav) = appTheme
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
    }

    /**
     * I know pure black is better but I love #8A000000 so much.
     * [R.dimen.material_emphasis_medium]
     */
    fun calculateOnColor(@ColorInt color: Int): Int {
        return if (ThemeColorUtils.isLightColor(color)) {
            ThemeColorUtils.adjustAlpha(Color.BLACK, 0.6f)
        } else {
            Color.WHITE
        }
    }

    fun configThemeForDark(context: Context, isDark: Boolean) {
        val (primary, _, sameStatusBar, enableNav) = context.appTheme
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
