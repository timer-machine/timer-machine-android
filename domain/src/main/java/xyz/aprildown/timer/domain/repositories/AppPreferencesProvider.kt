package xyz.aprildown.timer.domain.repositories

interface AppPreferencesProvider {
    fun getAppPreferences(): Map<String, String>
    fun applyAppPreferences(prefs: Map<String, String>)
}
