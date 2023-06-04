package xyz.aprildown.timer.flavor.google.backup.usecases

import android.content.Context
import androidx.core.net.toUri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import timber.log.Timber
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.data.ExportAppData
import xyz.aprildown.timer.domain.utils.AppConfig
import xyz.aprildown.timer.flavor.google.backup.CloudBackupWorker
import xyz.aprildown.timer.flavor.google.utils.causeFirstMessage
import xyz.aprildown.timer.flavor.google.utils.ensureNewFile
import xyz.aprildown.timer.flavor.google.utils.setUpFirebaseStorage
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Reusable
internal class CloudBackup @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @ApplicationContext private val applicationContext: Context,
    private val currentBackupState: CurrentBackupState,
    private val currentBackupStateError: CurrentBackupStateError,
    private val exportAppData: ExportAppData,
) : CoroutinesUseCase<Unit, Fruit<Unit>>(dispatcher) {

    init {
        setUpFirebaseStorage()
    }

    override suspend fun create(params: Unit): Fruit<Unit> {
        currentBackupState.set(CloudBackupState.Running)

        var fileToUpload: File? = null
        try {
            val targetFile = File(applicationContext.cacheDir, "upload.json").ensureNewFile()

            fileToUpload = targetFile

            targetFile.writeText(
                exportAppData(
                    ExportAppData.Params(
                        exportTimers = true,
                        exportSchedulers = true,
                        exportTimerStamps = true,
                        exportPreferences = true,
                    )
                )
            )

            yield()

            val storage = Firebase.storage
            val user = Firebase.auth.currentUser
            requireNotNull(user)

            yield()

            storage.reference
                .child(BACKUP_FOLDER_NAME)
                .child(user.uid)
                .child("${System.currentTimeMillis()}.json ")
                .putFile(
                    targetFile.toUri(),
                    StorageMetadata.Builder()
                        .setContentType("application/json")
                        .build()
                )
                .await()

            currentBackupState.set(CloudBackupState.UpToDate)

            return Fruit.Ripe(Unit)
        } catch (e: Exception) {
            currentBackupStateError.set(e.causeFirstMessage())
            currentBackupState.set(CloudBackupState.Error)
            return Fruit.Rotten(e)
        } finally {
            fileToUpload?.delete()
        }
    }

    suspend fun manualBackup(): Fruit<Unit> = withContext(dispatcher) {
        cancel(applicationContext, currentBackupState)

        execute(Unit)
    }

    companion object {
        const val BACKUP_FOLDER_NAME = "backup"
        const val CLOUD_BACKUP_LOG_TAG = "CLOUD_BACKUP"

        fun schedule(
            context: Context,
            currentBackupState: CurrentBackupState,
            immediate: Boolean = false
        ) {
            currentBackupState.set(CloudBackupState.Scheduled)

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    CloudBackupWorker.UNIQUE_NAME,
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<CloudBackupWorker>()
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.UNMETERED)
                                .setRequiresBatteryNotLow(true)
                                .build()
                        )
                        .let {
                            if (!AppConfig.openDebug && !immediate) {
                                it.setInitialDelay(1, TimeUnit.HOURS)
                            } else {
                                it
                            }
                        }
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            1,
                            TimeUnit.HOURS
                        )
                        .build()
                )
        }

        fun cancel(context: Context, currentBackupState: CurrentBackupState) {
            Timber.tag(CLOUD_BACKUP_LOG_TAG).i("Worker Canceled")

            currentBackupState.set(CloudBackupState.Required)

            WorkManager.getInstance(context)
                .cancelUniqueWork(CloudBackupWorker.UNIQUE_NAME)
        }
    }
}
