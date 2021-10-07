package xyz.aprildown.timer.data.repositories

import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.Reusable
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import javax.inject.Inject

@Reusable
internal class PreferencesRepoImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : PreferencesRepository {
    override suspend fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    override suspend fun getBoolean(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }

    override suspend fun setBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }
}
