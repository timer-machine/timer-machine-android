package xyz.aprildown.timer.data.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

internal class NewJobCreator : JobCreator {
    override fun create(tag: String): Job? = when {
        tag.startsWith(SchedulerJob.TAG_PREFIX) -> SchedulerJob()
        else -> null
    }
}
