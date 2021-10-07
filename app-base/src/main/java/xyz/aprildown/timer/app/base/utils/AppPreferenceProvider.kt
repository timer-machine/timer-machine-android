package xyz.aprildown.timer.app.base.utils

interface AppPreferenceProvider {
    fun getAppPreferences(): Map<String, String>
    fun applyAppPreferences(prefs: Map<String, String>)
}
