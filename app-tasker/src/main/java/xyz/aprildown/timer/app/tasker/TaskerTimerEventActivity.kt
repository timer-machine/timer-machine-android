package xyz.aprildown.timer.app.tasker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied
import dagger.Reusable
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.tasker.databinding.ActivityTaskerEditBinding
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.TaskerEventTrigger
import javax.inject.Inject
import xyz.aprildown.timer.app.base.R as RBase

@TaskerInputRoot
internal data class TaskerTimerEvent @JvmOverloads constructor(
    @TaskerInputField(key = "timerId") val timerId: Int = TimerEntity.NULL_ID,
    @TaskerInputField(key = "action") val action: String = TASKER_ACTION_START,
    @TaskerInputField(key = "timerName") val timerName: String? = null,
)

@TaskerOutputObject
internal class TaskerTimerEventOutput

@AndroidEntryPoint
internal class TaskerTimerEventActivity : BaseActivity(), TaskerPluginConfig<TaskerTimerEvent> {
    private val viewModel: TaskerEditViewModel by viewModels()

    @Inject
    lateinit var appNavigator: AppNavigator

    private lateinit var binding: ActivityTaskerEditBinding

    override val context: Context get() = applicationContext

    override val inputForTasker: TaskerInput<TaskerTimerEvent>
        get() = TaskerInput(
            regular = TaskerTimerEvent(
                timerId = viewModel.timerInfo.value?.id ?: TimerEntity.NULL_ID,
                timerName = viewModel.timerInfo.value?.name,
                action = when {
                    binding.radioTaskerEditStop.isChecked -> TASKER_ACTION_STOP
                    else -> TASKER_ACTION_START
                }
            )
        )

    override fun assignFromInput(input: TaskerInput<TaskerTimerEvent>) {
        when (input.regular.action) {
            TASKER_ACTION_STOP -> binding.radioTaskerEditStop.isChecked = true
            else -> binding.radioTaskerEditStart.isChecked = true
        }
        viewModel.loadTimer(input.regular.timerId)
    }

    private val helper: Helper by lazy { Helper(this) }

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
            binding.btnTaskerEditPickTimer.text =
                it?.name ?: getString(RBase.string.timer_pick_required)
        }

        binding.btnTaskerEditPickTimer.setOnClickListener {
            appNavigator.pickTimer(
                fm = supportFragmentManager,
                select = viewModel.timerInfo.value?.let { listOf(it.id) } ?: emptyList()
            ) {
                viewModel.loadTimer(it.timerInfo.first().id)
            }
        }

        helper.onCreate()
    }

    private class Helper(
        config: TaskerPluginConfig<TaskerTimerEvent>
    ) : TaskerPluginConfigHelper<TaskerTimerEvent, TaskerTimerEventOutput, TaskerTimerEventRunner>(
        config
    ) {
        override val inputClass: Class<TaskerTimerEvent> = TaskerTimerEvent::class.java
        override val outputClass: Class<TaskerTimerEventOutput> = TaskerTimerEventOutput::class.java
        override val runnerClass: Class<TaskerTimerEventRunner> = TaskerTimerEventRunner::class.java

        override val addDefaultStringBlurb: Boolean = false

        override fun addToStringBlurb(
            input: TaskerInput<TaskerTimerEvent>,
            blurbBuilder: StringBuilder
        ) {
            super.addToStringBlurb(input, blurbBuilder)
            val event = input.regular
            blurbBuilder.append(
                context.getString(
                    if (event.action == TASKER_ACTION_STOP) {
                        RBase.string.tasker_event_timer_stops
                    } else {
                        RBase.string.tasker_event_timer_starts
                    },
                    event.timerName
                )
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tasker_edit, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_tasker_edit_done -> {
                val targetTimerInfo = viewModel.timerInfo.value
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
}

internal class TaskerTimerEventRunner :
    TaskerPluginRunnerConditionEvent<TaskerTimerEvent, TaskerTimerEventOutput, TaskerTimerEvent>() {
    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<TaskerTimerEvent>,
        update: TaskerTimerEvent?
    ): TaskerPluginResultCondition<TaskerTimerEventOutput> {
        return if (update != null &&
            update.timerId != TimerEntity.NULL_ID &&
            update.timerId == input.regular.timerId &&
            update.action == input.regular.action
        ) {
            TaskerPluginResultConditionSatisfied(context)
        } else {
            TaskerPluginResultConditionUnsatisfied()
        }
    }
}

@Reusable
internal class TaskerEventTriggerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : TaskerEventTrigger {
    override fun timerStart(id: Int) {
        TaskerTimerEventActivity::class.java.requestQuery(
            context,
            TaskerTimerEvent(timerId = id, action = TASKER_ACTION_START)
        )
    }

    override fun timerEnd(id: Int) {
        TaskerTimerEventActivity::class.java.requestQuery(
            context,
            TaskerTimerEvent(timerId = id, action = TASKER_ACTION_STOP)
        )
    }
}
