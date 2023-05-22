package xyz.aprildown.timer.flavor.google.backup

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.TaskStackBuilder
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.Observer
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.deweyreed.tools.arch.Event
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.utils.AppInfoNotificationManager
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.flavor.google.BillingActivity
import xyz.aprildown.timer.flavor.google.BillingSupervisor
import xyz.aprildown.timer.flavor.google.backup.usecases.AutoCloudBackup
import xyz.aprildown.timer.flavor.google.backup.usecases.CloudBackup
import xyz.aprildown.timer.flavor.google.backup.usecases.CloudBackupState
import xyz.aprildown.timer.flavor.google.backup.usecases.CurrentBackupState
import xyz.aprildown.timer.flavor.google.backup.usecases.CurrentBackupStateError
import javax.inject.Provider
import kotlin.coroutines.resume
import xyz.aprildown.timer.app.base.R as RBase

@HiltWorker
internal class CloudBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cloudBackup: CloudBackup,
    private val currentBackupState: CurrentBackupState,
    private val currentBackupStateError: Provider<CurrentBackupStateError>,
    private val autoCloudBackup: Provider<AutoCloudBackup>,
    private val appNavigator: Provider<AppNavigator>,
    private val appTracker: AppTracker,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.Main) {
        val context = applicationContext

        Timber.tag(CloudBackup.CLOUD_BACKUP_LOG_TAG).i("Worker Run")

        val hasSignedIn = Firebase.auth.currentUser != null
        if (!hasSignedIn) {
            disableAutoBackup()
            currentBackupStateError.get().set(
                context.getString(RBase.string.cloud_backup_account_issue_notif_title)
            )
            currentBackupState.set(CloudBackupState.Error)
            showAccountErrorNotification()
            return@withContext Result.failure()
        }

        val hasSubscription = try {
            withTimeout(10_000) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val supervisor = BillingSupervisor(context, requestBackupSubState = true)
                    supervisor.supervise()

                    var hasResumed = false
                    fun resumeOnceOrIgnore(result: Boolean) {
                        if (hasResumed) return
                        hasResumed = true
                        cont.resume(result)
                    }

                    val stateObserver = object : Observer<Boolean> {
                        override fun onChanged(value: Boolean) {
                            supervisor.backupSubState.removeObserver(this)
                            supervisor.endConnection()

                            resumeOnceOrIgnore(value)
                        }
                    }
                    supervisor.backupSubState.observeForever(stateObserver)

                    val errorObserver = object : Observer<Event<BillingSupervisor.Error>> {
                        override fun onChanged(value: Event<BillingSupervisor.Error>) {
                            supervisor.error.removeObserver(this)
                            supervisor.endConnection()

                            resumeOnceOrIgnore(false)
                        }
                    }
                    supervisor.error.observeForever(errorObserver)

                    cont.invokeOnCancellation {
                        supervisor.backupSubState.removeObserver(stateObserver)
                        supervisor.error.removeObserver(errorObserver)
                        supervisor.endConnection()
                    }
                }
            }
        } catch (e: Exception) {
            appTracker.trackError(e)
            return@withContext Result.retry()
        }

        if (!hasSubscription) {
            disableAutoBackup()
            currentBackupStateError.get().set(
                context.getString(RBase.string.cloud_backup_billing_issue_notif_title)
            )
            currentBackupState.set(CloudBackupState.Error)
            showSubscriptionErrorNotification()
            return@withContext Result.failure()
        }

        try {
            when (val fruit = cloudBackup()) {
                is Fruit.Ripe -> {
                    Timber.tag(CloudBackup.CLOUD_BACKUP_LOG_TAG).i("Worker Done")
                    Result.success()
                }
                is Fruit.Rotten -> {
                    throw fruit.exception
                }
            }
        } catch (e: Exception) {
            Timber.tag(CloudBackup.CLOUD_BACKUP_LOG_TAG).e(e)

            val stateError = currentBackupStateError.get()
            val error = stateError.get()
            if (!error.isNullOrBlank()) {
                stateError.set(
                    buildString {
                        append(context.getString(RBase.string.cloud_backup_state_scheduled))
                        append("\n")
                        append(context.getString(RBase.string.cloud_backup_recent_error))
                        append(": ")
                        append(error)
                    }
                )
            }

            Result.retry()
        }
    }

    private fun showAccountErrorNotification() {
        val context = applicationContext
        AppInfoNotificationManager(context)
            .notify(
                titleRes = RBase.string.cloud_backup_account_issue_notif_title,
                despRes = RBase.string.cloud_backup_account_issue_notif_desp,
                pendingIntent = newBillingPendingIntent()
            )
    }

    private fun showSubscriptionErrorNotification() {
        val context = applicationContext
        AppInfoNotificationManager(context)
            .notify(
                titleRes = RBase.string.cloud_backup_billing_issue_notif_title,
                despRes = RBase.string.cloud_backup_billing_issue_notif_desp,
                pendingIntent = newBillingPendingIntent()
            )
    }

    private fun newBillingPendingIntent(): PendingIntent {
        val pendingIntent = TaskStackBuilder.create(applicationContext)
            .addNextIntent(appNavigator.get().getMainIntent())
            .addNextIntent(BillingActivity.getIntent(applicationContext))
            .getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        requireNotNull(pendingIntent)
        return pendingIntent
    }

    private fun disableAutoBackup() {
        autoCloudBackup.get().set(false)
    }

    companion object {
        const val UNIQUE_NAME = "cloud_backup"
    }
}
