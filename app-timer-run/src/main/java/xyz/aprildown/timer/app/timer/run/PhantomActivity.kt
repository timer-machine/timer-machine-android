package xyz.aprildown.timer.app.timer.run

import android.os.Bundle
import androidx.core.content.ContextCompat
import com.github.deweyreed.tools.anko.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.domain.utils.Constants
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class PhantomActivity : BaseActivity() {

    @Inject
    lateinit var findTimerInfo: FindTimerInfo

    @Inject
    lateinit var appNavigator: AppNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            when (intent?.action) {
                ACTION_SHORTCUT_CREATED -> toast(RBase.string.shortcut_created)
                Constants.ACTION_START -> {
                    val id = intent?.getIntExtra(Constants.EXTRA_TIMER_ID, TimerEntity.NULL_ID)
                    if (id != null &&
                        id != TimerEntity.NULL_ID &&
                        runBlocking { findTimerInfo(id) != null }
                    ) {
                        ContextCompat.startForegroundService(
                            this,
                            MachineService.startTimingIntent(this, id)
                        )
                        if (intent.getBooleanExtra(EXTRA_ONE_SETTING, false)) {
                            startActivity(appNavigator.getOneIntent(timerId = id))
                        } else {
                            toast(RBase.string.shortcut_timer_started)
                        }
                    }
                }
            }
        } finally {
            finish()
        }
    }

    companion object {
        const val ACTION_SHORTCUT_CREATED = "action_shortcut_created"
        const val EXTRA_ONE_SETTING = "extra_one_setting"
    }
}
