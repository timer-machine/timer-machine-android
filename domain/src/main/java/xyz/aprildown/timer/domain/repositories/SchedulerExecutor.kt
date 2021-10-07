package xyz.aprildown.timer.domain.repositories

import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.usecases.scheduler.SetSchedulerEnable

interface SchedulerExecutor {
    /**
     * @return scheduled job's trigger time
     */
    fun schedule(scheduler: SchedulerEntity): SetSchedulerEnable.Result

    /**
     * @return canceled job count
     */
    fun cancel(scheduler: SchedulerEntity): SetSchedulerEnable.Result
}
