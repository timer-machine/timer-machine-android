package xyz.aprildown.timer.app.intro.start

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import com.github.deweyreed.tools.helper.color
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.show
import xyz.aprildown.timer.app.base.utils.setTime
import xyz.aprildown.timer.app.intro.R
import xyz.aprildown.timer.app.intro.databinding.LayoutIntroStartEditBinding
import xyz.aprildown.timer.app.intro.databinding.LayoutIntroStartListBinding
import xyz.aprildown.timer.app.intro.databinding.LayoutIntroStartRunBinding
import xyz.aprildown.timer.app.intro.showInteractionIndicator
import xyz.aprildown.timer.component.key.DurationPicker
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.presentation.edit.EditViewModel
import com.mikepenz.materialize.R as RMaterialize
import xyz.aprildown.timer.app.base.R as RBase
import xyz.aprildown.timer.app.timer.edit.R as RTimerEdit
import xyz.aprildown.timer.app.timer.list.R as RTimerList
import xyz.aprildown.timer.app.timer.one.R as RTimerOne

internal class Welcome : StartListInstruction(
    despRes = RBase.string.intro_start_welcome
)

internal class OurPlan : StartListInstruction(
    despRes = RBase.string.intro_start_our_plan
)

internal class CreateTimer : StartListInstruction(
    despRes = RBase.string.intro_start_create_timer,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartListBinding) {
        binding.fabIntroStartList.run {
            showInteractionIndicator()
            setOnClickListener {
                markAsCompleted()
            }
        }
    }
}

internal class EnterLoop : StartEditInstruction(
    despRes = RBase.string.intro_start_enter_loop,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        val context = binding.root.context
        binding.viewEditNameLoop.loopView.run {
            showInteractionIndicator(context.color(RMaterialize.color.md_light_blue_400))
            isEnabled = true
            setText(EditViewModel.defaultLoop.toString())
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                    Unit

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString() == "5") {
                        // markAsCompleted triggers the watcher so we remove the watcher first.
                        removeTextChangedListener(this)
                        markAsCompleted()

                        clearFocus()
                        context.getSystemService<InputMethodManager>()
                            ?.hideSoftInputFromWindow(windowToken, 0)
                    }
                }
            }
            addTextChangedListener(watcher)
            setTag(R.id.tag_loop_change_listener, watcher)
        }
        binding.stepIntroStartEdit1.lengthTextView.setTime(60_000L)
    }
}

internal class StepCard : StartEditInstruction(
    despRes = RBase.string.intro_start_step_card
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        binding.stepIntroStartEdit1.run {
            cardView.showInteractionIndicator()
            lengthTextView.setTime(60_000L)
        }
    }
}

internal class StepTime : StartEditInstruction(
    despRes = RBase.string.intro_start_step_time,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        val context = binding.root.context
        binding.stepIntroStartEdit1.lengthTextView.run {
            setTime(60_000L)
            showInteractionIndicator(context.color(RMaterialize.color.md_light_blue_400))
            setOnClickListener {
                DurationPicker(context) { hours, minutes, seconds ->
                    val time = (hours * 3600L + minutes * 60L + seconds) * 1000L
                    setTime(time)
                    if (time == 180_000L) {
                        markAsCompleted()
                    }
                }.show()
            }
        }
    }
}

internal class Notifier : StartEditInstruction(
    despRes = RBase.string.intro_start_notifier
)

internal class AddNotifier : StartEditInstruction(
    despRes = RBase.string.intro_start_add_notifier,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        binding.root.findViewById<View>(RTimerEdit.id.btnAddNotifier).run {
            showInteractionIndicator(context.color(RMaterialize.color.md_light_blue_400))
            setOnClickListener {
                markAsCompleted()
            }
        }
    }
}

internal class AddReminder : StartEditInstruction(
    despRes = RBase.string.intro_start_add_reminder,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        binding.stepIntroStartEdit2.show()
        val behaviourLayout = binding.stepIntroStartEdit2.behaviourLayout
        behaviourLayout.addButton.showInteractionIndicator()
        behaviourLayout.setBehaviours(emptyList())
        behaviourLayout.setBehaviourAddedOrRemovedCallback {
            val behaviors = behaviourLayout.getBehaviours()
            if (behaviors.any { it.type == BehaviourType.MUSIC } &&
                behaviors.any { it.type == BehaviourType.VIBRATION }
            ) {
                markAsCompleted()
            }
        }
    }
}

internal class ReminderUsage : StartEditInstruction(
    despRes = RBase.string.intro_start_reminder_usage
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        binding.stepIntroStartEdit2.show()
    }
}

internal class AddWalkStep : StartEditInstruction(
    despRes = RBase.string.intro_start_add_walk_step,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        binding.stepIntroStartEdit2.show()
        binding.root.findViewById<View>(RTimerEdit.id.btnAddStep).run {
            showInteractionIndicator(context.color(RMaterialize.color.md_light_blue_400))
            setOnClickListener {
                markAsCompleted()
            }
        }
    }
}

internal class WalkStepTime : StartEditInstruction(
    despRes = RBase.string.intro_start_walk_step_name,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        val context = binding.root.context
        binding.stepIntroStartEdit2.show()
        binding.stepIntroStartEdit3.show()
        binding.stepIntroStartEdit3.lengthTextView.run {
            setTime(60_000L)
            showInteractionIndicator(context.color(RMaterialize.color.md_light_blue_400))
            setOnClickListener {
                DurationPicker(context) { hours, minutes, seconds ->
                    val time = (hours * 3600L + minutes * 60L + seconds) * 1000L
                    setTime(time)
                    if (time == 120_000L) {
                        markAsCompleted()
                    }
                }.show()
            }
        }
    }
}

internal class WalkNotifier : StartEditInstruction(
    despRes = RBase.string.intro_start_walk_notifier,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        binding.stepIntroStartEdit2.show()
        binding.stepIntroStartEdit3.show()
        binding.root.findViewById<View>(RTimerEdit.id.btnAddNotifier).run {
            showInteractionIndicator(context.color(RMaterialize.color.md_light_blue_400))
            setOnClickListener {
                markAsCompleted()
            }
        }
    }
}

internal class TimerDone : StartEditInstruction(
    despRes = RBase.string.intro_start_timer_done,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartEditBinding) {
        val context = binding.root.context
        binding.stepIntroStartEdit2.show()
        binding.stepIntroStartEdit3.show()
        binding.stepIntroStartEdit4.show()
        binding.viewIntroStartEditSaveIndicator.showInteractionIndicator(
            context.color(RMaterialize.color.md_light_blue_400)
        )
        binding.toolbar.menu.findItem(R.id.action_save_timer)?.setOnMenuItemClickListener {
            markAsCompleted()
            true
        }
    }
}

internal class RunTimer : StartListInstruction(
    despRes = RBase.string.intro_start_run_timer,
    requireAction = true
) {
    override fun setUpViews(binding: LayoutIntroStartListBinding) {
        binding.run {
            root.findViewById<View>(RTimerList.id.cardTimer).run {
                show()
                showInteractionIndicator()
                setOnClickListener {
                    markAsCompleted()
                }
            }
            viewIntroStartListEmpty.gone()
        }
    }
}

internal class TimerTime : StartRunInstruction(
    despRes = RBase.string.intro_start_timer_time
) {
    override fun setUpViews(binding: LayoutIntroStartRunBinding) {
        binding.root.findViewById<View>(RTimerOne.id.cardOneTopInfo).showInteractionIndicator()
    }
}

internal class TimerSteps : StartRunInstruction(
    despRes = RBase.string.intro_start_timer_steps
) {
    override fun setUpViews(binding: LayoutIntroStartRunBinding) {
        binding.root.findViewById<View>(RTimerOne.id.listOneSteps).showInteractionIndicator()
    }
}

internal class TimerButtons : StartRunInstruction(
    despRes = RBase.string.intro_start_timer_buttons
) {
    override fun setUpViews(binding: LayoutIntroStartRunBinding) {
        val context = binding.root.context
        binding.root.findViewById<View>(RTimerOne.id.fiveActionsOne).showInteractionIndicator(
            context.color(RMaterialize.color.md_light_blue_400)
        )
    }
}

internal class Finish : StartRunInstruction(
    despRes = RBase.string.intro_start_finish
)
