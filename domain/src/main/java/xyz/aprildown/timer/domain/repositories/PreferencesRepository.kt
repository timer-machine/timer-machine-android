package xyz.aprildown.timer.domain.repositories

interface PreferencesRepository {
    suspend fun contains(key: String): Boolean
    suspend fun getBoolean(key: String, default: Boolean): Boolean
    suspend fun setBoolean(key: String, value: Boolean)
}
