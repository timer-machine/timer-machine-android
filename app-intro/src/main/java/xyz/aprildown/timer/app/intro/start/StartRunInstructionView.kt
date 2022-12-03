package xyz.aprildown.timer.app.intro.start

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.deweyreed.tools.anko.dp
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneFourActions
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneTimeSize
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.intro.Instruction
import xyz.aprildown.timer.app.intro.InstructionView
import xyz.aprildown.timer.app.intro.R
import xyz.aprildown.timer.app.intro.clearInteractionIndicator
import xyz.aprildown.timer.app.intro.databinding.LayoutIntroStartRunBinding
import xyz.aprildown.timer.app.timer.one.FiveActionsView
import xyz.aprildown.timer.app.timer.one.OneFragment
import xyz.aprildown.timer.app.timer.one.step.StepListView
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getNiceLoopString
import xyz.aprildown.timer.app.timer.one.R as RTimerOne

internal class StartRunInstructionView : InstructionView<LayoutIntroStartRunBinding> {
    override val layoutRes: Int = R.layout.layout_intro_start_run
    override lateinit var binding: LayoutIntroStartRunBinding

    override fun initBinding(context: Context, parent: ViewGroup) {
        binding = LayoutIntroStartRunBinding.inflate(LayoutInflater.from(context), parent, false)
    }

    override fun reset() {
        val context = binding.root.context
        val timer = Instruction.getInitialSampleTimer(context)
        val index = TimerIndex.Step(0, 0)
        binding.root.run {
            findViewById<View>(RTimerOne.id.cardOneTopInfo).clearInteractionIndicator()

            findViewById<TextView>(RTimerOne.id.textOneTime).run {
                textSize = dp(context.oneOneTimeSize)
                text = (timer.steps.first() as StepEntity.Step).length.produceTime()
            }

            findViewById<TextView>(RTimerOne.id.textOneLoop)
                .text = index.getNiceLoopString(max = timer.loop)

            findViewById<StepListView>(RTimerOne.id.listOneSteps).run {
                clearInteractionIndicator()
                setTimer(timer)
                toIndex(index)
            }

            findViewById<FiveActionsView>(RTimerOne.id.fiveActionsOne).run {
                clearInteractionIndicator()
                withActions(OneFragment.getFourActionsFromKeys(context.oneOneFourActions))
            }
        }
    }
}
