package xyz.aprildown.timer.domain.repositories

interface PreferencesRepository {
    suspend fun contains(key: String): Boolean

    suspend fun getBoolean(key: String, default: Boolean): Boolean
    suspend fun setBoolean(key: String, value: Boolean)

    suspend fun getInt(key: String, default: Int): Int
    suspend fun setInt(key: String, value: Int)

    suspend fun getLong(key: String, default: Long): Long
    suspend fun setLong(key: String, value: Long)

    suspend fun getNullableString(key: String, default: String? = null): String?
    suspend fun getNonNullString(key: String, default: String): String
    suspend fun setString(key: String, value: String?)
}
