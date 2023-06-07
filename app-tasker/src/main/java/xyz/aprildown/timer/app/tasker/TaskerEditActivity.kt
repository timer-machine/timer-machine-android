package xyz.aprildown.timer.app.tasker

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputInfos
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.tasker.databinding.ActivityTaskerEditBinding
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import javax.inject.Inject
import kotlin.properties.Delegates
import xyz.aprildown.timer.app.base.R as RBase

@AndroidEntryPoint
class TaskerEditActivity : BaseActivity(), TaskerPluginConfig<Unit> {

    private val viewModel: TaskerEditViewModel by viewModels()

    @Inject
    lateinit var appNavigator: AppNavigator

    private lateinit var binding: ActivityTaskerEditBinding

    private var currentTimerInfo: TimerInfo? by Delegates.observable(null) { _, _, newValue ->
        binding.btnTaskerEditPickTimer.text =
            newValue?.name ?: getString(RBase.string.timer_pick_required)
    }

    private val helper by lazy { TaskerTimerHelper(this) }
    override val context: Context get() = applicationContext
    override val inputForTasker: TaskerInput<Unit>
        get() = TaskerInput(
            Unit,
            TaskerInputInfos.fromBundle(
                this,
                Unit,
                bundleOf(
                    TASKER_TIMER_ID to (currentTimerInfo?.id ?: TimerEntity.NULL_ID),
                    TASKER_ACTION to if (binding.radioTaskerEditStart.isChecked) {
                        TASKER_ACTION_START
                    } else {
                        TASKER_ACTION_STOP
                    }
                )
            )
        )

    override fun assignFromInput(input: TaskerInput<Unit>) {
        val bundle = input.dynamic.bundle

        if (bundle.getTaskerTimerAction() == TASKER_ACTION_STOP) {
            binding.radioTaskerEditStop.isChecked = true
        } else {
            binding.radioTaskerEditStart.isChecked = true
        }

        viewModel.loadTimer(bundle.getTaskerTimerId())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskerEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.run {
            setDisplayShowHomeEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        viewModel.timerInfo.observe(this) {
            currentTimerInfo = it
        }

        binding.btnTaskerEditPickTimer.setOnClickListener {
            appNavigator.pickTimer(
                fm = supportFragmentManager,
                select = currentTimerInfo?.id?.let { listOf(it) } ?: emptyList()
            ) {
                currentTimerInfo = it.timerInfo.first()
            }
        }

        helper.onCreate()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tasker_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_tasker_edit_done -> {
                val targetTimerInfo = currentTimerInfo
                if (targetTimerInfo != null) {
                    helper.finishForTasker()
                }
                return true
            }
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class TaskerTimerHelper(
        config: TaskerPluginConfig<Unit>,
    ) : TaskerPluginConfigHelper<Unit, Unit, TaskerTimerRunner>(config) {
        override val runnerClass: Class<TaskerTimerRunner> = TaskerTimerRunner::class.java
        override val inputClass: Class<Unit> get() = Unit::class.java
        override val outputClass: Class<Unit> get() = Unit::class.java

        override fun addToStringBlurb(input: TaskerInput<Unit>, blurbBuilder: StringBuilder) {
            super.addToStringBlurb(input, blurbBuilder)
            blurbBuilder.append(
                "%s %s".format(
                    getString(
                        if (binding.radioTaskerEditStart.isChecked) {
                            RBase.string.start
                        } else {
                            RBase.string.stop
                        }
                    ),
                    currentTimerInfo?.name
                )
            )
        }
    }
}

internal class TaskerTimerRunner : TaskerPluginRunnerAction<Unit, Unit>() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TaskerTimerRunnerEntryPointInterface {
        fun getPresenter(): TaskerRunPresenter
    }

    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        val presenter = EntryPoints.get(
            context.applicationContext,
            TaskerTimerRunnerEntryPointInterface::class.java
        ).getPresenter()

        val bundle = input.dynamic.bundle
        val timerId = bundle.getTaskerTimerId()
        if (!runBlocking { presenter.isValidTimerId(timerId) }) {
            return TaskerPluginResultError(IllegalArgumentException("Unable to find the timer"))
        }
        when (bundle.getTaskerTimerAction()) {
            TASKER_ACTION_START -> {
                ContextCompat.startForegroundService(
                    context,
                    presenter.start(timerId)
                )
            }
            TASKER_ACTION_STOP -> {
                fun sendStopIntent() {
                    ContextCompat.startForegroundService(
                        context,
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

        return TaskerPluginResultSucess()
    }
}
