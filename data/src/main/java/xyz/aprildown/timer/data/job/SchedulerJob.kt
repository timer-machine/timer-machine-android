package xyz.aprildown.timer.data.job

import android.content.Context
import android.content.Intent
import com.evernote.android.job.Job
import com.evernote.android.job.JobManager
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import xyz.aprildown.timer.domain.entities.SchedulerEntity

/**
 * We have to make this open because it controls some common actions.
 */
class SchedulerJob : Job() {

    override fun onRunJob(params: Params): Result {
        val extras = params.extras
        if (!extras.containsKey(EXTRA_ID) || !extras.containsKey(EXTRA_ACTION)) {
            return Result.FAILURE
        } else {
            context.sendBroadcast(
                Intent(RECEIVE_JOB_ACTION)
                    .setClassName(
                        context.packageName,
                        "xyz.aprildown.timer.app.timer.run.receiver.SchedulerReceiver"
                    )
                    .setFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra(EXTRA_ID, extras.getInt(EXTRA_ID, 0))
                    .putExtra(EXTRA_ACTION, extras.getInt(EXTRA_ACTION, 0))
            )

            return Result.SUCCESS
        }
    }

    companion object {
        const val RECEIVE_JOB_ACTION = "xyz.aprildown.timer.data.job.scheduler"

        const val TAG_PREFIX = "scheduler"
        const val EXTRA_ID = "id"
        const val EXTRA_ACTION = "action"

        private val Int.jobTag: String
            get() = "${TAG_PREFIX}_$this"

        internal fun scheduleAJob(scheduler: SchedulerEntity): Long {
            val nextTime = scheduler.getNextFireTime().timeInMillis
            JobRequest.Builder(scheduler.id.jobTag)
                .setExact(nextTime - System.currentTimeMillis())
                .setUpdateCurrent(true)
                .setExtras(
                    PersistableBundleCompat().apply {
                        putInt(EXTRA_ID, scheduler.id)
                        putInt(EXTRA_ACTION, scheduler.action)
                    }
                )
                .build()
                .schedule()
            return nextTime
        }

        internal fun cancelAJob(context: Context, id: Int): Int {
            return JobManager.create(context).cancelAllForTag(id.jobTag)
        }
    }
}
