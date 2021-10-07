package xyz.aprildown.timer.app.tasker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.BaseActivity
import xyz.aprildown.timer.app.tasker.databinding.ActivityTaskerEditBinding
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.presentation.tasker.TaskerEditViewModel
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class TaskerEditActivity : BaseActivity() {

    private val viewModel: TaskerEditViewModel by viewModels()

    @Inject
    lateinit var appNavigator: AppNavigator

    private lateinit var binding: ActivityTaskerEditBinding

    private var currentTimerInfo: TimerInfo? by Delegates.observable(null) { _, _, newValue ->
        binding.btnTaskerEditPickTimer.text =
            newValue?.name ?: getString(R.string.timer_pick_required)
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

        binding.radioTaskerEditStart.isChecked = true

        intent?.getBundleExtra(EXTRA_BUNDLE)?.let { bundle ->
            viewModel.loadTimer(bundle.getInt(TASKER_TIMER_ID, TimerEntity.NULL_ID))

            if (bundle.getString(TASKER_ACTION) == TASKER_ACTION_STOP) {
                binding.radioTaskerEditStop.isChecked = true
            } else {
                binding.radioTaskerEditStart.isChecked = true
            }
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
                    val shouldStart = binding.radioTaskerEditStart.isChecked
                    val intent = Intent()
                        .putExtra(
                            EXTRA_STRING_BLURB,
                            "%s %s".format(
                                getString(
                                    if (shouldStart) R.string.start else R.string.stop
                                ),
                                targetTimerInfo.name
                            )
                        )
                        .putExtra(
                            EXTRA_BUNDLE,
                            bundleOf(
                                TASKER_TIMER_ID to targetTimerInfo.id,
                                TASKER_ACTION to
                                    if (shouldStart) TASKER_ACTION_START else TASKER_ACTION_STOP
                            )
                        )
                    setResult(Activity.RESULT_OK, intent)
                } else {
                    setResult(Activity.RESULT_CANCELED)
                }
                finish()
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
