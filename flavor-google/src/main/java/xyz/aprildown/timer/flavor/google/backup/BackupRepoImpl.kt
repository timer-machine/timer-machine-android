package xyz.aprildown.timer.flavor.google.backup

import android.content.Context
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.flavor.google.backup.usecases.AutoCloudBackup
import xyz.aprildown.timer.flavor.google.backup.usecases.CloudBackup
import xyz.aprildown.timer.flavor.google.backup.usecases.CloudBackupState
import xyz.aprildown.timer.flavor.google.backup.usecases.CurrentBackupState
import javax.inject.Inject

@Reusable
class BackupRepoImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppDataRepository.BackupRepository {

    @Inject
    internal lateinit var autoCloudBackup: AutoCloudBackup

    @Inject
    internal lateinit var currentBackupState: CurrentBackupState

    override suspend fun onAppDataChanged() {
        currentBackupState.set(CloudBackupState.Required)
        // Auto Backup is only enabled after purchases and signing in.
        // Even if the user's subscription is expired, we'll check it in the Worker.
        if (autoCloudBackup.get()) {
            CloudBackup.schedule(context, currentBackupState)
        }
    }
}
