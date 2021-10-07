package xyz.aprildown.timer.presentation.scheduler

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.SchedulerRepeatMode
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.usecases.scheduler.AddScheduler
import xyz.aprildown.timer.domain.usecases.scheduler.GetScheduler
import xyz.aprildown.timer.domain.usecases.scheduler.SaveScheduler
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.presentation.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class EditSchedulerViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val findTimerInfo: FindTimerInfo,
    private val getScheduler: GetScheduler,
    private val addScheduler: AddScheduler,
    private val saveSchedulerUseCase: SaveScheduler
) : BaseViewModel(mainDispatcher) {

    private var isNewScheduler = false
    private var schedulerHash = 0

    private val _schedulerWithTimerInfo = MutableLiveData<Pair<SchedulerEntity, TimerInfo?>>()
    val schedulerWithTimerInfo: LiveData<Pair<SchedulerEntity, TimerInfo?>> =
        _schedulerWithTimerInfo

    fun load(schedulerId: Int) = launch {
        if (schedulerWithTimerInfo.value == null) {
            isNewScheduler = schedulerId == SchedulerEntity.NEW_ID

            val scheduler: SchedulerEntity? = if (isNewScheduler) {
                null
            } else {
                getScheduler(schedulerId)?.also {
                    schedulerHash = it.hashCode()
                }
            }

            if (scheduler == null) {
                _schedulerWithTimerInfo.value = SchedulerEntity(
                    SchedulerEntity.NULL_ID,
                    0, "", 0, 0, 0,
                    SchedulerRepeatMode.ONCE,
                    List(7) { false },
                    0
                ) to null
            } else {
                _schedulerWithTimerInfo.value = scheduler to findTimerInfo(scheduler.timerId)
            }
        }
    }

    fun isTheSameScheduler(newScheduler: SchedulerEntity): Boolean {
        // We'll ignore new schedulers because the checking is complicated.
        return isNewScheduler || newScheduler.hashCode() == schedulerHash
    }

    fun saveScheduler(
        newScheduler: SchedulerEntity,
        onDone: (() -> Unit)? = null
    ) = launch {
        if (isNewScheduler) {
            addScheduler(newScheduler)
        } else {
            saveSchedulerUseCase(newScheduler)
        }
        onDone?.invoke()
    }
}
