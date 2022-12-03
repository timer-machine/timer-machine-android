package xyz.aprildown.timer.presentation.timer

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.github.deweyreed.tools.arch.Event
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.folder.AddFolder
import xyz.aprildown.timer.domain.usecases.folder.DeleteFolder
import xyz.aprildown.timer.domain.usecases.folder.FolderSortByRule
import xyz.aprildown.timer.domain.usecases.folder.GetFolders
import xyz.aprildown.timer.domain.usecases.folder.RecentFolder
import xyz.aprildown.timer.domain.usecases.folder.UpdateFolder
import xyz.aprildown.timer.domain.usecases.home.TipManager
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.timer.AddTimer
import xyz.aprildown.timer.domain.usecases.timer.ChangeTimerFolder
import xyz.aprildown.timer.domain.usecases.timer.DeleteTimer
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import xyz.aprildown.timer.domain.usecases.timer.GetTimerInfoFlow
import xyz.aprildown.timer.domain.usecases.timer.ShareTimer
import xyz.aprildown.timer.presentation.BaseViewModel
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import xyz.aprildown.timer.presentation.stream.StreamState
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val getTimerInfoFlow: GetTimerInfoFlow,
    private val addTimer: AddTimer,
    private val getTimer: GetTimer,
    private val changeTimerFolder: ChangeTimerFolder,
    private val deleteTimer: Lazy<DeleteTimer>,
    private val shareTimer: Lazy<ShareTimer>,

    private val getFolders: GetFolders,
    private val addFolder: AddFolder,
    private val updateFolder: UpdateFolder,
    private val deleteFolder: Lazy<DeleteFolder>,

    private val folderSortByRule: FolderSortByRule,
    private val recentFolder: RecentFolder,

    private val tipManager: TipManager,

    private val streamMachineIntentProvider: StreamMachineIntentProvider,
) : BaseViewModel(mainDispatcher) {

    private val _editEvent = MutableLiveData<Event<Int>>()
    val editEvent: LiveData<Event<Int>> = _editEvent
    private val _intentEvent = MutableLiveData<Event<Intent>>()
    val intentEvent: LiveData<Event<Intent>> = _intentEvent

    private val _allFolders: MutableLiveData<List<FolderEntity>> = MutableLiveData()
    val allFolders: LiveData<List<FolderEntity>> = _allFolders

    private val _currentFolderId = MutableLiveData<Long>()
    val currentFolderId: LiveData<Long> = _currentFolderId

    val currentFolder: LiveData<FolderEntity?> =
        allFolders.asFlow().combine(currentFolderId.asFlow()) { folders, id ->
            folders.find { it.id == id }
        }.asLiveData()

    private val currentSortBy = MutableLiveData<FolderSortBy>()

    val timerInfo: LiveData<List<TimerInfo>> =
        currentFolderId.asFlow().combine(currentSortBy.asFlow()) { id, by ->
            id to by
        }.asLiveData().switchMap { (folderId, sortBy) ->
            getTimerInfoFlow.get(folderId, sortBy).asLiveData()
        }

    private val _shareStringEvent = MutableLiveData<Event<Fruit<String>>>()
    val shareStringEvent: LiveData<Event<Fruit<String>>> = _shareStringEvent

    val tips: LiveData<Int> = tipManager.getTipFlow(this).asLiveData()

    init {
        launch {
            // Order matters.
            refreshFolders()
            val recentFolderId = recentFolder.get()
            _currentFolderId.value = if (
                (_allFolders.value ?: emptyList()).find { it.id == recentFolderId } != null
            ) {
                recentFolderId
            } else {
                FolderEntity.FOLDER_DEFAULT
            }
            currentSortBy.value = folderSortByRule.get()
        }
    }

    private suspend fun refreshFolders() {
        _allFolders.value = getFolders.invoke()
    }

    fun changeFolder(folderId: Long) {
        if (folderId == currentFolderId.value) return

        _currentFolderId.value = folderId
        launch {
            recentFolder.set(folderId)
        }
    }

    fun createNewFolder(name: String) {
        launch {
            val newId = addFolder(FolderEntity(FolderEntity.NEW_ID, name))
            refreshFolders()
            changeFolder(newId)
        }
    }

    fun changeCurrentFolderName(newName: String) {
        val currentId = currentFolderId.value ?: return
        launch {
            updateFolder(FolderEntity(id = currentId, name = newName))
            refreshFolders()
            _currentFolderId.value = currentId
        }
    }

    fun changeSortBy(newSortBy: FolderSortBy) {
        currentSortBy.value = newSortBy
        launch {
            folderSortByRule.set(newSortBy)
        }
    }

    fun moveTimerToFolder(timerId: Int, folderId: Long) {
        launch {
            changeTimerFolder(ChangeTimerFolder.Params(timerId = timerId, folderId = folderId))
        }
    }

    fun deleteCurrentFolder() {
        val currentId = currentFolderId.value ?: return
        launch(NonCancellable) {
            deleteFolder.get().invoke(currentId)
            refreshFolders()
            if (currentId != FolderEntity.FOLDER_DEFAULT && currentId != FolderEntity.FOLDER_TRASH) {
                changeFolder(FolderEntity.FOLDER_DEFAULT)
            }
        }
    }

    fun getBindIntent(): Intent = streamMachineIntentProvider.bindIntent()

    fun startPauseAction(id: Int, state: StreamState) {
        if (state.isReset || state.isPaused) {
            _intentEvent.value = Event(streamMachineIntentProvider.startIntent(id))
        } else if (state.isRunning) {
            _intentEvent.value = Event(streamMachineIntentProvider.pauseIntent(id))
        }
    }

    fun stopAction(id: Int, state: StreamState) {
        if (!state.isReset) {
            _intentEvent.value = Event(streamMachineIntentProvider.resetIntent(id))
        }
    }

    // fun plusOneAction(id: Int, state: StreamState) {
    //     if (!state.isReset) {
    //         _intentEvent.value = Event(streamMachineIntentProvider.plusOneIntent(id))
    //     }
    // }
    //
    // fun nextAction(id: Int, state: StreamState) {
    //     if (!state.isReset) {
    //         _intentEvent.value = Event(streamMachineIntentProvider.increIntent(id))
    //     }
    // }

    fun addNewTimer() {
        _editEvent.value = Event(TimerEntity.NEW_ID)
    }

    fun openTimerEditScreen(timerId: Int) {
        _editEvent.value = Event(timerId)
    }

    fun duplicate(timerId: Int) = launch {
        val timer = getTimer(timerId)
        if (timer != null) {
            addTimer(timer.copy(id = TimerEntity.NEW_ID))
        }
    }

    fun deleteTimer(id: Int) = launch(NonCancellable) {
        deleteTimer.get().invoke(id)
    }

    fun generateTimerString(timerId: Int) {
        launch {
            val timer = getTimer(timerId)
            requireNotNull(timer)
            _shareStringEvent.value = Event(
                shareTimer.get().shareAsString(
                    listOf(
                        timer.copy(
                            id = TimerEntity.NEW_ID,
                            folderId = FolderEntity.FOLDER_DEFAULT
                        )
                    )
                )
            )
        }
    }

    fun consumeTip(tip: Int) {
        viewModelScope.launch(NonCancellable) {
            tipManager.consumeTip(tip)
        }
    }
}
