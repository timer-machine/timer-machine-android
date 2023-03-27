package xyz.aprildown.timer.domain.repositories

import androidx.collection.arrayMapOf

open class TestPreferencesRepository : PreferencesRepository {
    private val map = arrayMapOf<Any, Any>()

    override suspend fun contains(key: String): Boolean {
        return map.contains(key)
    }

    override suspend fun getBoolean(key: String, default: Boolean): Boolean {
        return map.getOrDefault(key, default) as Boolean
    }

    override suspend fun setBoolean(key: String, value: Boolean) {
        map[key] = value
    }

    override suspend fun getInt(key: String, default: Int): Int {
        return map.getOrDefault(key, default) as Int
    }

    override suspend fun setInt(key: String, value: Int) {
        map[key] = value
    }

    override suspend fun getLong(key: String, default: Long): Long {
        return map.getOrDefault(key, default) as Long
    }

    override suspend fun setLong(key: String, value: Long) {
        map[key] = value
    }

    override suspend fun getNullableString(key: String, default: String?): String? {
        return map.getOrDefault(key, default) as String?
    }

    override suspend fun getNonNullString(key: String, default: String): String {
        return map.getOrDefault(key, default) as String
    }

    override suspend fun setString(key: String, value: String?) {
        map[key] = value
    }
}
