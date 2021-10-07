package xyz.aprildown.timer.app.intro.start

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import xyz.aprildown.timer.app.intro.Instruction
import xyz.aprildown.timer.app.intro.InstructionView
import xyz.aprildown.timer.app.intro.R
import xyz.aprildown.timer.app.intro.clearInteractionIndicator
import xyz.aprildown.timer.app.intro.databinding.LayoutIntroStartListBinding
import xyz.aprildown.timer.app.intro.showTooltip
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.show

internal class StartListInstructionView : InstructionView<LayoutIntroStartListBinding> {

    override val layoutRes: Int = R.layout.layout_intro_start_list
    override lateinit var binding: LayoutIntroStartListBinding

    override fun initBinding(context: Context, parent: ViewGroup) {
        binding = LayoutIntroStartListBinding.inflate(LayoutInflater.from(context), parent, false)
    }

    override fun reset() {
        val context = binding.root.context
        val timer = Instruction.getInitialSampleTimer(context)
        binding.run {
            root.findViewById<View>(R.id.cardTimer).run {
                gone()
                clearInteractionIndicator()
                setOnClickListener(null)
            }
            root.findViewById<TextView>(R.id.textTimerName).text = timer.name
            root.findViewById<View>(R.id.imageTimerStartPause).setOnClickListener {
                it.showTooltip(context.getString(R.string.intro_start_quick_start_pause))
            }
            viewIntroStartListEmpty.show()

            fabIntroStartList.run {
                clearInteractionIndicator()
                setOnClickListener(null)
            }
        }
    }
}
