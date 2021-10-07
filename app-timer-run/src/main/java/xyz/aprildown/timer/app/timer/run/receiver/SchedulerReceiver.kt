package xyz.aprildown.timer.app.timer.run.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.timer.run.MachineService
import xyz.aprildown.timer.data.job.SchedulerJob
import xyz.aprildown.timer.data.job.SchedulerJob.Companion.EXTRA_ACTION
import xyz.aprildown.timer.data.job.SchedulerJob.Companion.EXTRA_ID
import xyz.aprildown.timer.data.job.SchedulerJob.Companion.RECEIVE_JOB_ACTION
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.presentation.scheduler.SchedulerReceiverPresenter
import javax.inject.Inject

/**
 * This package's name is used in the [SchedulerJob].
 */
@AndroidEntryPoint
class SchedulerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var presenter: SchedulerReceiverPresenter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RECEIVE_JOB_ACTION) return
        val c = context.applicationContext
        val schedulerId = intent.getIntExtra(EXTRA_ID, SchedulerEntity.NULL_ID)
        when (intent.getIntExtra(EXTRA_ACTION, -1)) {
            SchedulerEntity.ACTION_START -> {
                val timerId = presenter.handleFiredScheduler(schedulerId)
                if (timerId != TimerEntity.NULL_ID && presenter.isValidTimerId(timerId)) {
                    ContextCompat.startForegroundService(
                        c,
                        MachineService.scheduleTimerStartIntent(context, timerId)
                    )
                }
            }
            SchedulerEntity.ACTION_END -> {
                val timerId = presenter.handleFiredScheduler(schedulerId)
                if (timerId != TimerEntity.NULL_ID && presenter.isValidTimerId(timerId)) {
                    fun sendStopIntent() {
                        ContextCompat.startForegroundService(
                            c,
                            MachineService.scheduleTimerEndIntent(context, timerId)
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.getSystemService<NotificationManager>()?.let {
                            if (it.activeNotifications.isNotEmpty()) {
                                sendStopIntent()
                            }
                        }
                    } else {
                        sendStopIntent()
                    }
                }
            }
        }
    }

    companion object {
        fun intent(context: Context, schedulerId: Int, action: Int): Intent =
            Intent(context, SchedulerReceiver::class.java)
                .setAction(RECEIVE_JOB_ACTION)
                .putExtra(EXTRA_ID, schedulerId)
                .putExtra(EXTRA_ACTION, action)
    }
}
