package xyz.aprildown.timer.presentation.timer

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.usecases.folder.FolderSortByRule
import xyz.aprildown.timer.domain.usecases.folder.GetFolders
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.timer.GetTimerInfo
import xyz.aprildown.timer.presentation.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class TimerPickerViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val getFolders: GetFolders,
    private val getTimerInfo: GetTimerInfo,
    private val folderSortByRule: FolderSortByRule
) : BaseViewModel(mainDispatcher) {

    val folderTimers: LiveData<Map<FolderEntity, List<TimerInfo>>> = liveData {
        val folders = getFolders.invoke()
        val map = mutableMapOf<FolderEntity, List<TimerInfo>>()

        val sortBy = folderSortByRule.get()
        folders.forEach {
            map[it] = getTimerInfo(GetTimerInfo.Params(folderId = it.id, sortBy = sortBy))
        }

        emit(map)
    }
}
