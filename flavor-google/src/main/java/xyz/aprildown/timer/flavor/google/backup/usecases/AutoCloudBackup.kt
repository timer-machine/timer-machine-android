package xyz.aprildown.timer.flavor.google.backup.usecases

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.aprildown.timer.flavor.google.R
import javax.inject.Inject

@Reusable
internal class AutoCloudBackup @Inject constructor(
    @ApplicationContext context: Context,
    private val sharedPreferences: SharedPreferences
) {

    private val autoBackupKey = context.getString(R.string.backup_key_auto_cloud_backup)

    fun get(): Boolean {
        if (!sharedPreferences.contains(autoBackupKey)) return false
        return sharedPreferences.getBoolean(autoBackupKey, false)
    }

    fun set(value: Boolean) {
        sharedPreferences.edit {
            putBoolean(autoBackupKey, value)
        }
    }
}
