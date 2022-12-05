package xyz.aprildown.timer.data.repositories

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService<AlarmManager>()
            // The permission seems to have been granted by default.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                am?.canScheduleExactAlarms() == false
            ) {
                return SetSchedulerEnable.Result.Failed("No permission to schedule exact alarms")
            }
        }
        return when (scheduler.action) {
            SchedulerEntity.ACTION_START, SchedulerEntity.ACTION_END -> {
                SetSchedulerEnable.Result.Scheduled(SchedulerJob.scheduleAJob(scheduler))
            }
            else -> SetSchedulerEnable.Result.Failed("Invalid action")
        }
    }

    override fun cancel(scheduler: SchedulerEntity): SetSchedulerEnable.Result {
        return SetSchedulerEnable.Result.Canceled(SchedulerJob.cancelAJob(context, scheduler.id))
    }
}
