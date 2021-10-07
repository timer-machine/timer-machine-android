package xyz.aprildown.timer.flavor.google.backup.usecases

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
import dagger.Reusable
import timber.log.Timber
import xyz.aprildown.timer.flavor.google.R
import javax.inject.Inject

internal enum class CloudBackupState(@StringRes val despId: Int) {
    Required(R.string.cloud_backup_state_required),
    Scheduled(R.string.cloud_backup_state_scheduled),
    Running(R.string.cloud_backup_state_running),
    UpToDate(R.string.cloud_backup_state_up_to_date),
    Error(R.string.cloud_backup_state_error);

    val canBackupNow: Boolean get() = this != Running && this != UpToDate

    companion object {
        const val PREF_CLOUD_BACKUP_STATE = "pref_cloud_backup_state"
        const val PREF_CLOUD_BACKUP_ERROR = "pref_cloud_backup_error"
    }
}

@Reusable
internal class CurrentBackupState @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    fun get(): CloudBackupState {
        return CloudBackupState
            .values()[sharedPreferences.getInt(CloudBackupState.PREF_CLOUD_BACKUP_STATE, 0)]
    }

    fun set(value: CloudBackupState) {
        Timber.tag(CloudBackup.CLOUD_BACKUP_LOG_TAG).i("State ${value.name}")
        sharedPreferences.edit {
            putInt(CloudBackupState.PREF_CLOUD_BACKUP_STATE, value.ordinal)
        }
    }
}

@Reusable
internal class CurrentBackupStateError @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    fun get(): String? {
        return sharedPreferences.getString(CloudBackupState.PREF_CLOUD_BACKUP_ERROR, null)
    }

    fun set(value: String?) {
        sharedPreferences.edit {
            putString(CloudBackupState.PREF_CLOUD_BACKUP_ERROR, value)
        }
    }
}
