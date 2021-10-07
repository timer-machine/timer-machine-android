package xyz.aprildown.timer.presentation.scheduler

import kotlinx.coroutines.runBlocking
import xyz.aprildown.timer.domain.entities.SchedulerRepeatMode
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.usecases.scheduler.GetScheduler
import xyz.aprildown.timer.domain.usecases.scheduler.SetSchedulerEnable
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import javax.inject.Inject

class SchedulerReceiverPresenter @Inject constructor(
    private val findTimerInfo: FindTimerInfo,
    private val getScheduler: GetScheduler,
    private val setSchedulerEnable: SetSchedulerEnable
) {

    fun isValidTimerId(timerId: Int): Boolean {
        return runBlocking {
            findTimerInfo(timerId) != null
        }
    }

    fun handleFiredScheduler(schedulerId: Int): Int = runBlocking {
        val scheduler = getScheduler.execute(schedulerId)
        if (scheduler != null) {
            setSchedulerEnable
                .execute(
                    SetSchedulerEnable.Params(
                        scheduler.id,
                        if (scheduler.repeatMode != SchedulerRepeatMode.ONCE) 1 else 0
                    )
                )
        }
        scheduler?.timerId ?: TimerEntity.NULL_ID
    }
}
