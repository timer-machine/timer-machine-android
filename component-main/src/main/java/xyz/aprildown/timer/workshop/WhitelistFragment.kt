package xyz.aprildown.timer.workshop

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import xyz.aprildown.timer.app.base.utils.openWebsiteWithWarning
import xyz.aprildown.tools.helper.startActivitySafely
import xyz.aprildown.timer.app.base.R as RBase

class WhitelistFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()

        setPreferencesFromResource(R.xml.pref_whitelist, rootKey)

        findPreference<PreferenceCategory>("key_links")?.addPreference(
            Preference(context).apply {
                withDefaultSettings()
                title = "Don't kill my app!"
                setSummary(RBase.string.whitelist_don_t_kill_desp)
                setOnPreferenceClickListener {
                    context.openWebsiteWithWarning(
                        "https://dontkillmyapp.com?app=${getString(RBase.string.app_name)}"
                    )
                    true
                }
            }
        )

        findPreference<PreferenceCategory>("key_settings")?.let { category ->
            findAllWhitelistItems(context).forEach { action ->
                category.addPreference(
                    Preference(context).apply {
                        withDefaultSettings()
                        title = action.title
                        setOnPreferenceClickListener {
                            context.startActivitySafely(
                                Intent.createChooser(action.intent, null),
                                wrongMessageRes = RBase.string.no_action_found
                            )
                            true
                        }
                    }
                )
            }

            category.addPreference(
                Preference(context).apply {
                    withDefaultSettings()
                    setTitle(RBase.string.whitelist_ignore_battery_optimization)
                    setOnPreferenceClickListener {
                        context.startActivitySafely(
                            Intent.createChooser(
                                @Suppress("BatteryLife")
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData("package:${context.packageName}".toUri()),
                                null
                            ),
                            wrongMessageRes = RBase.string.no_action_found
                        )

                        true
                    }
                }
            )
        }
    }

    private fun Preference.withDefaultSettings() {
        isPersistent = false
        isSingleLineTitle = false
        isIconSpaceReserved = false
    }
}

private data class WhitelistAction(val title: String, val intent: Intent)

/**
 * This isn't complete and workable at all time.
 */
@Suppress("SpellCheckingInspection")
private fun findAllWhitelistItems(context: Context): List<WhitelistAction> {
    val appName = context.getString(RBase.string.app_name)
    val packageName = context.packageName

    val result = mutableListOf<WhitelistAction>()

    when (Build.MANUFACTURER.lowercase()) {
        "huawei" -> {
            result += WhitelistAction(
                title = context.getString(RBase.string.whitelist_settings_template, 1),
                intent = Intent("huawei.intent.action.HSM_BOOTAPP_MANAGER")
            )
            result += WhitelistAction(
                title = context.getString(RBase.string.whitelist_settings_template, 2),
                intent = Intent().setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                )
            )
        }
        "xiaomi" -> {
            result += WhitelistAction(
                title = context.getString(RBase.string.whitelist_settings_template, 1),
                intent = Intent("miui.intent.action.OP_AUTO_START")
                    .addCategory(Intent.CATEGORY_DEFAULT)
            )
            result += WhitelistAction(
                title = context.getString(RBase.string.whitelist_settings_template, 2),
                intent = Intent().setComponent(
                    ComponentName(
                        "com.miui.powerkeeper",
                        "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                    )
                )
                    .putExtra("package_name", packageName)
                    .putExtra("package_label", appName)
            )
        }
        "meizu" -> {
            result += WhitelistAction(
                title = context.getString(RBase.string.whitelist_settings_template, 1),
                intent = Intent("com.meizu.safe.security.SHOW_APPSEC")
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .putExtra("packageName", packageName)
            )
            result += WhitelistAction(
                title = context.getString(RBase.string.whitelist_settings_template, 2),
                intent = Intent().setComponent(
                    ComponentName(
                        "com.meizu.safe",
                        "com.meizu.safe.powerui.PowerAppPermissionActivity"
                    )
                )
            )
        }
        "vivo" -> {
            result += WhitelistAction(
                title = context.getString(RBase.string.whitelist_settings_template, 1),
                intent = Intent().setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                )
            )
            result += WhitelistAction(
                title = context.getString(RBase.string.whitelist_settings_template, 2),
                intent = Intent().setComponent(
                    ComponentName(
                        "com.vivo.abe",
                        "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
                    )
                )
            )
        }
    }

    return result
}
