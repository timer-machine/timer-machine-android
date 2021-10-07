package xyz.aprildown.timer.data.job

import android.app.Application
import com.evernote.android.job.JobManager

fun Application.initJob() {
    JobManager.create(this).addJobCreator(NewJobCreator())
}
