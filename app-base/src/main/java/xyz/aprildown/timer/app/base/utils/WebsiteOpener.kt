package xyz.aprildown.timer.app.base.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.deweyreed.tools.helper.isDarkTheme
import com.github.deweyreed.tools.helper.isNetworkCheap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.data.DarkTheme
import xyz.aprildown.timer.app.base.data.PreferenceData.appTheme
import xyz.aprildown.timer.app.base.databinding.DialogWebsiteWarningBinding
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.tools.helper.safeSharedPreference

private fun Context.openWebsiteWithCustomTabs(url: String) {
    val darkTheme = DarkTheme(this)
    val appTheme = appTheme
    val cti = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .apply {
            newDynamicTheme.run {
                setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(colorPrimary)
                        .setSecondaryToolbarColor(colorPrimaryVariant)
                        .setNavigationBarColor(colorNavigationBar)
                        .build()
                )
            }
        }
        .setColorScheme(
            when {
                darkTheme.darkThemeValue == DarkTheme.DARK_THEME_SYSTEM_DEFAULT ->
                    CustomTabsIntent.COLOR_SCHEME_SYSTEM
                resources.isDarkTheme ->
                    CustomTabsIntent.COLOR_SCHEME_DARK
                else ->
                    CustomTabsIntent.COLOR_SCHEME_LIGHT
            }
        )
        .setColorSchemeParams(
            CustomTabsIntent.COLOR_SCHEME_LIGHT,
            CustomTabColorSchemeParams.Builder()
                .setToolbarColor(appTheme.colorPrimary)
                .setSecondaryToolbarColor(appTheme.colorSecondary)
                .apply {
                    if (appTheme.enableNav) {
                        setNavigationBarColor(appTheme.colorPrimary)
                    }
                }
                .build()
        )
        .setColorSchemeParams(
            CustomTabsIntent.COLOR_SCHEME_DARK,
            CustomTabColorSchemeParams.Builder()
                .apply {
                    val color =
                        AppThemeUtils.calculateToolbarColorDuringNight(this@openWebsiteWithCustomTabs)
                    setToolbarColor(color)
                    setSecondaryToolbarColor(color)
                    if (appTheme.enableNav) {
                        setNavigationBarColor(color)
                    }
                }
                .build()
        )
        .setUrlBarHidingEnabled(true)
        .build()
    val uri = url.toUri()
    try {
        cti.launchUrl(this, uri)
    } catch (_: ActivityNotFoundException) {
        ContextCompat.startActivity(this, Intent.createChooser(cti.intent.setData(uri), null), null)
    }
}

private const val KEY_SHOW_WEBSITE_WARNING = "warning_website"

fun Context.openWebsiteWithWarning(url: String) {
    if (isNetworkCheap() || !safeSharedPreference.getBoolean(KEY_SHOW_WEBSITE_WARNING, true)) {
        openWebsiteWithCustomTabs(url)
    } else {
        val binding = DialogWebsiteWarningBinding.inflate(LayoutInflater.from(this))
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                openWebsiteWithCustomTabs(url)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        dialog.setOnDismissListener {
            if (binding.check.isChecked) {
                safeSharedPreference.edit {
                    putBoolean(KEY_SHOW_WEBSITE_WARNING, false)
                }
            }
        }
    }
}
