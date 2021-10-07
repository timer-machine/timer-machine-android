package xyz.aprildown.timer.data.repositories

import android.content.Context
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.aprildown.timer.data.job.SchedulerJob
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.repositories.SchedulerExecutor
import xyz.aprildown.timer.domain.usecases.scheduler.SetSchedulerEnable
import javax.inject.Inject

/**
 * We have to make this open because the receiver's package name and class name
 * are injected from the app module.
 */
@Reusable
class SchedulerExecutorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SchedulerExecutor {

    override fun schedule(scheduler: SchedulerEntity): SetSchedulerEnable.Result {
        return when (scheduler.action) {
            SchedulerEntity.ACTION_START, SchedulerEntity.ACTION_END -> {
                SetSchedulerEnable.Result.Scheduled(SchedulerJob.scheduleAJob(scheduler))
            }
            else -> SetSchedulerEnable.Result.Failed
        }
    }

    override fun cancel(scheduler: SchedulerEntity): SetSchedulerEnable.Result {
        return SetSchedulerEnable.Result.Canceled(SchedulerJob.cancelAJob(context, scheduler.id))
    }
}
