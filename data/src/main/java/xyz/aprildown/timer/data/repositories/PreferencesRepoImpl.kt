package xyz.aprildown.timer.data.repositories

import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.deweyreed.tools.helper.getNonNullString
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import javax.inject.Inject

@Reusable
internal class PreferencesRepoImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val sharedPreferences: SharedPreferences
) : PreferencesRepository {
    override suspend fun contains(key: String): Boolean = withContext(ioDispatcher) {
        sharedPreferences.contains(key)
    }

    override suspend fun getBoolean(
        key: String,
        default: Boolean
    ): Boolean = withContext(ioDispatcher) {
        sharedPreferences.getBoolean(key, default)
    }

    override suspend fun setBoolean(key: String, value: Boolean) = withContext(ioDispatcher) {
        sharedPreferences.edit { putBoolean(key, value) }
    }

    override suspend fun getInt(key: String, default: Int): Int = withContext(ioDispatcher) {
        sharedPreferences.getInt(key, default)
    }

    override suspend fun setInt(key: String, value: Int) = withContext(ioDispatcher) {
        sharedPreferences.edit { putInt(key, value) }
    }

    override suspend fun getLong(key: String, default: Long): Long = withContext(ioDispatcher) {
        sharedPreferences.getLong(key, default)
    }

    override suspend fun setLong(key: String, value: Long) = withContext(ioDispatcher) {
        sharedPreferences.edit { putLong(key, value) }
    }

    override suspend fun getNullableString(
        key: String,
        default: String?
    ): String? = withContext(ioDispatcher) {
        sharedPreferences.getString(key, default)
    }

    override suspend fun getNonNullString(
        key: String,
        default: String
    ): String = withContext(ioDispatcher) {
        sharedPreferences.getNonNullString(key, default)
    }

    override suspend fun setString(key: String, value: String?) = withContext(ioDispatcher) {
        sharedPreferences.edit { putString(key, value) }
    }
}
