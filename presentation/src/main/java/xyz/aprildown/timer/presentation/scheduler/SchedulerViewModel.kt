package xyz.aprildown.timer.presentation.scheduler

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.deweyreed.tools.arch.Event
import com.github.deweyreed.tools.helper.toInt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.scheduler.DeleteScheduler
import xyz.aprildown.timer.domain.usecases.scheduler.GetSchedulers
import xyz.aprildown.timer.domain.usecases.scheduler.SetSchedulerEnable
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.presentation.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val getSchedulers: GetSchedulers,
    private val findTimerInfo: FindTimerInfo,
    private val setSchedulerEnable: SetSchedulerEnable,
    private val deleteScheduler: DeleteScheduler,
) : BaseViewModel(mainDispatcher) {

    private val _schedulerWithTimerInfo = MutableLiveData<List<Pair<SchedulerEntity, TimerInfo?>>>()
    val schedulerWithTimerInfo: LiveData<List<Pair<SchedulerEntity, TimerInfo?>>> =
        _schedulerWithTimerInfo

    private val _scheduleEvent = MutableLiveData<Event<SetSchedulerEnable.Result>>()
    val scheduleEvent: LiveData<Event<SetSchedulerEnable.Result>> = _scheduleEvent

    fun load() = launch {
        val result = mutableListOf<Pair<SchedulerEntity, TimerInfo?>>()
        getSchedulers().forEach { scheduler ->
            result += scheduler to findTimerInfo(scheduler.timerId)
        }
        _schedulerWithTimerInfo.value = result
    }

    fun toggleSchedulerState(id: Int, enabled: Boolean): Job = launch {
        _scheduleEvent.value = Event(
            setSchedulerEnable(SetSchedulerEnable.Params(id, enabled.toInt()))
        )
    }

    fun delete(id: Int) {
        launch {
            deleteScheduler(id)
        }
    }
}
