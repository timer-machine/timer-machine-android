package xyz.aprildown.timer.flavor.google.backup

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.flavor.google.BillingSupervisor
import xyz.aprildown.timer.flavor.google.backup.usecases.AutoCloudBackup
import xyz.aprildown.timer.flavor.google.backup.usecases.CloudBackup
import xyz.aprildown.timer.flavor.google.backup.usecases.CurrentBackupState
import xyz.aprildown.timer.flavor.google.backup.usecases.GetRestoreReferences
import xyz.aprildown.timer.flavor.google.backup.usecases.RestoreFromCloud
import xyz.aprildown.timer.presentation.BaseViewModel
import xyz.aprildown.tools.arch.Event
import xyz.aprildown.tools.arch.EventObserver
import javax.inject.Inject

/**
 * Retain this ViewModel in an Activity lifecycle because it's not expensive.
 */
@HiltViewModel
internal class CloudBackupViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val application: Application,
    private val currentBackupState: CurrentBackupState,
    private val autoCloudBackup: AutoCloudBackup,
    private val cloudBackup: Lazy<CloudBackup>,
    private val getRestoreReferences: Lazy<GetRestoreReferences>,
    private val restoreFromCloud: Lazy<RestoreFromCloud>
) : BaseViewModel(mainDispatcher) {

    var isPromotionShown = false

    val billingSupervisor = BillingSupervisor(
        application,
        requestBackupSubState = true,
    )
    private val backupSubEventObserver: EventObserver<Unit>
    private val backupSubStateObserver: Observer<Boolean>

    init {
        billingSupervisor.supervise()
        backupSubEventObserver = EventObserver {
            tryToSetUpForNewSubscriber()
        }
        billingSupervisor.backupSubEvent.observeForever(backupSubEventObserver)
        backupSubStateObserver = Observer {
            if (!it) {
                autoCloudBackup.set(false)
            }
        }
        billingSupervisor.backupSubState.observeForever(backupSubStateObserver)
    }

    override fun onCleared() {
        super.onCleared()
        billingSupervisor.endConnection()
        billingSupervisor.backupSubEvent.removeObserver(backupSubEventObserver)
        billingSupervisor.backupSubState.removeObserver(backupSubStateObserver)
    }

    // region Backup

    private var manualCloudBackupJob: Job? = null

    private val _manualCloudBackupResult: MutableLiveData<Fruit<Unit>> = MutableLiveData()
    val manualCloudBackupResult: LiveData<Fruit<Unit>> = _manualCloudBackupResult

    fun manualCloudBackup() {
        manualCloudBackupJob?.cancel()
        manualCloudBackupJob = launch {
            _manualCloudBackupResult.value = cloudBackup.get().manualBackup()
            manualCloudBackupJob = null
        }
    }

    fun cancelCloudBackup() {
        manualCloudBackupJob?.cancel()
        manualCloudBackupJob = null
    }

    private val storageReference = MutableLiveData<StorageReference>()
    val userFiles: LiveData<Fruit<List<Pair<StorageReference, StorageMetadata>>>> =
        storageReference.switchMap {
            liveData {
                emit(getRestoreReferences.get().get(it))
            }
        }

    fun requestUserFiles(reference: StorageReference) {
        storageReference.value = reference
    }

    val autoBackupEnabledEvent = MutableLiveData<Event<Unit>>()

    fun tryToSetUpForNewSubscriber() {
        if (Firebase.auth.currentUser != null &&
            billingSupervisor.backupSubState.value == true
        ) {
            autoCloudBackup.set(true)
            autoBackupEnabledEvent.value = Event(Unit)
            CloudBackup.schedule(application, currentBackupState, immediate = true)
        }
    }

    // endregion Backup

    // region Restore

    private var restoreJob: Job? = null
    private val _restoreResult: MutableLiveData<Fruit<Unit>> = MutableLiveData()
    val restoreResult: LiveData<Fruit<Unit>> = _restoreResult

    fun startRestoring(reference: StorageReference) {
        restoreJob?.cancel()
        restoreJob = launch {
            _restoreResult.value = restoreFromCloud.get().restore(reference)
            restoreJob = null
        }
    }

    fun cancelRestoring() {
        restoreJob?.cancel()
        restoreJob = null
    }

    // endregion
}
