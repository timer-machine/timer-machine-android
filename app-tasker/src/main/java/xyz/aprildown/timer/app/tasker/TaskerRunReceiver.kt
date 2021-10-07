package xyz.aprildown.timer.app.tasker

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.presentation.tasker.TaskerRunPresenter
import javax.inject.Inject

@AndroidEntryPoint
internal class TaskerRunReceiver : BroadcastReceiver() {

    @Inject
    lateinit var presenter: TaskerRunPresenter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_SETTING || !intent.hasExtra(EXTRA_BUNDLE)) return

        val c = context.applicationContext
        val bundle = intent.getBundleExtra(EXTRA_BUNDLE) ?: return
        val timerId = bundle.getInt(TASKER_TIMER_ID, SchedulerEntity.NULL_ID)
        if (!presenter.isValidTimerId(timerId)) return
        when (bundle.getString(TASKER_ACTION)) {
            TASKER_ACTION_START -> {
                ContextCompat.startForegroundService(
                    c,
                    presenter.start(timerId)
                )
            }
            TASKER_ACTION_STOP -> {
                fun sendStopIntent() {
                    ContextCompat.startForegroundService(
                        c,
                        presenter.stop(timerId)
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
