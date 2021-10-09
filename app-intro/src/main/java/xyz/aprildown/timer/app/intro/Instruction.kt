package xyz.aprildown.timer.app.intro

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.viewbinding.ViewBinding
import xyz.aprildown.timer.domain.entities.MusicAction
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.VibrationAction
import xyz.aprildown.timer.app.base.R as RBase

internal class InstructionManager {

    interface Callback {
        fun onNewInstruction(instruction: Instruction<out ViewBinding>)
        fun onFinalInstruction()
        fun onNoMoreInstruction()
    }

    private val instructions = mutableListOf<Instruction<out ViewBinding>>()
    var currentIndex = -1
        private set

    var callback: Callback? = null

    val isTheFirst: Boolean get() = currentIndex <= 0
    val isTheLast: Boolean get() = currentIndex >= instructions.lastIndex
    val currentProgressText: CharSequence get() = "${currentIndex + 1}/${instructions.size}"

    fun withInstructions(newInstructions: List<Instruction<out ViewBinding>>) {
        instructions.clear()
        instructions.addAll(newInstructions)
        currentIndex = 0

        to(currentIndex)
    }

    fun next() {
        if (currentIndex + 1 in instructions.indices) {
            to(currentIndex + 1)
        } else {
            callback?.onNoMoreInstruction()
        }
    }

    fun previous() {
        if (currentIndex - 1 in instructions.indices) {
            to(currentIndex - 1)
        } else {
            callback?.onNoMoreInstruction()
        }
    }

    fun to(index: Int) {
        if (index in instructions.indices) {
            currentIndex = index
            callback?.onNewInstruction(
                instructions[index].apply {
                    instructionManager = this@InstructionManager
                }
            )
            if (isTheLast) {
                callback?.onFinalInstruction()
            }
        }
    }
}

internal interface InstructionView<out VB : ViewBinding> {
    @get:LayoutRes
    val layoutRes: Int
    val binding: VB
    fun initBinding(context: Context, parent: ViewGroup)
    fun reset()
}

internal abstract class Instruction<VB : ViewBinding>(
    @LayoutRes val layoutRes: Int,
    @StringRes val despRes: Int,
    val requireAction: Boolean = false
) {
    lateinit var instructionManager: InstructionManager

    open fun setUpViews(binding: VB): Unit = Unit

    fun markAsCompleted() {
        instructionManager.next()
    }

    abstract fun createInstructionView(context: Context, parent: ViewGroup): InstructionView<VB>

    companion object {
        fun getInitialSampleTimer(context: Context): TimerEntity {
            return TimerEntity(
                id = TimerEntity.NEW_ID,
                name = context.getString(RBase.string.intro_start_sample_timer_name),
                loop = 5,
                steps = listOf(
                    StepEntity.Step(
                        label = context.getString(RBase.string.intro_start_sample_timer_step1),
                        length = 180_000L
                    ),
                    StepEntity.Step(
                        label = context.getString(RBase.string.intro_start_sample_timer_step2),
                        length = 10_000L,
                        behaviour = listOf(
                            MusicAction().toBehaviourEntity(),
                            VibrationAction().toBehaviourEntity(),
                        ),
                        type = StepType.NOTIFIER
                    ),
                    StepEntity.Step(
                        label = context.getString(RBase.string.intro_start_sample_timer_step3),
                        length = 120_000L
                    ),
                    StepEntity.Step(
                        label = context.getString(RBase.string.intro_start_sample_timer_step4),
                        length = 10_000L,
                        behaviour = listOf(
                            MusicAction().toBehaviourEntity(),
                            VibrationAction().toBehaviourEntity(),
                        ),
                        type = StepType.NOTIFIER
                    ),
                )
            )
        }
    }
}
