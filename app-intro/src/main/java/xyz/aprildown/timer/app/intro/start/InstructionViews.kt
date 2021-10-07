package xyz.aprildown.timer.app.intro.start

import android.content.Context
import android.view.ViewGroup
import xyz.aprildown.timer.app.intro.Instruction
import xyz.aprildown.timer.app.intro.InstructionView
import xyz.aprildown.timer.app.intro.R
import xyz.aprildown.timer.app.intro.databinding.LayoutIntroStartEditBinding
import xyz.aprildown.timer.app.intro.databinding.LayoutIntroStartListBinding
import xyz.aprildown.timer.app.intro.databinding.LayoutIntroStartRunBinding

internal abstract class StartListInstruction(
    despRes: Int,
    requireAction: Boolean = false
) : Instruction<LayoutIntroStartListBinding>(
    layoutRes = R.layout.layout_intro_start_list,
    despRes = despRes,
    requireAction = requireAction
) {
    override fun createInstructionView(
        context: Context,
        parent: ViewGroup
    ): InstructionView<LayoutIntroStartListBinding> {
        return StartListInstructionView().also {
            it.initBinding(context, parent)
        }
    }
}

internal abstract class StartEditInstruction(
    despRes: Int,
    requireAction: Boolean = false
) : Instruction<LayoutIntroStartEditBinding>(
    layoutRes = R.layout.layout_intro_start_edit,
    despRes = despRes,
    requireAction = requireAction
) {
    override fun createInstructionView(
        context: Context,
        parent: ViewGroup
    ): InstructionView<LayoutIntroStartEditBinding> {
        return StartEditInstructionView().also {
            it.initBinding(context, parent)
        }
    }
}

internal abstract class StartRunInstruction(
    despRes: Int,
    requireAction: Boolean = false
) : Instruction<LayoutIntroStartRunBinding>(
    layoutRes = R.layout.layout_intro_start_run,
    despRes = despRes,
    requireAction = requireAction
) {
    override fun createInstructionView(
        context: Context,
        parent: ViewGroup
    ): InstructionView<LayoutIntroStartRunBinding> {
        return StartRunInstructionView().also {
            it.initBinding(context, parent)
        }
    }
}
