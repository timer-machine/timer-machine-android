package xyz.aprildown.timer.presentation.tasker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.presentation.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class TaskerEditViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val findTimerInfo: FindTimerInfo
) : BaseViewModel(mainDispatcher) {
    private val _timerId = MutableLiveData<Int>()
    val timerInfo: LiveData<TimerInfo?>
        get() = _timerId.switchMap {
            requireNotNull(it)
            liveData { emit(findTimerInfo(it)) }
        }

    fun loadTimer(timerId: Int) {
        _timerId.value = timerId
    }
}
