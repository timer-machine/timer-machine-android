package xyz.aprildown.timer.presentation.stream

import android.content.Intent

/**
 * This structure comes with an assumption: there is no "group in a group".
 */
sealed class TimerIndex {

    data object Start : TimerIndex()

    data class Step(val loopIndex: Int, val stepIndex: Int) : TimerIndex()

    data class Group(val loopIndex: Int, val stepIndex: Int, val groupStepIndex: Step) :
        TimerIndex()

    data object End : TimerIndex()
}

private const val KEY_INDEX_TYPE = "timer_index_type"
private const val INDEX_TYPE_START = "timer_index_type_start"
private const val INDEX_TYPE_STEP = "timer_index_type_step"
private const val INDEX_TYPE_GROUP = "timer_index_type_group"
private const val INDEX_TYPE_END = "timer_index_type_end"

private const val VALUE_LOOP_INDEX = "timer_index_value_loop_index"
private const val VALUE_STEP_INDEX = "timer_index_value_step_index"
private const val VALUE_GROUP_STEP_LOOP_INDEX = "timer_index_value_group_step_loop_index"
private const val VALUE_GROUP_STEP_STEP_INDEX = "timer_index_value_group_step_step_index"

fun Intent.getTimerIndex(): TimerIndex? {
    return when (getStringExtra(KEY_INDEX_TYPE)) {
        INDEX_TYPE_START -> TimerIndex.Start
        INDEX_TYPE_STEP -> TimerIndex.Step(
            loopIndex = getIntExtra(VALUE_LOOP_INDEX, 0),
            stepIndex = getIntExtra(VALUE_STEP_INDEX, 0)
        )
        INDEX_TYPE_GROUP -> TimerIndex.Group(
            loopIndex = getIntExtra(VALUE_LOOP_INDEX, 0),
            stepIndex = getIntExtra(VALUE_STEP_INDEX, 0),
            groupStepIndex = TimerIndex.Step(
                loopIndex = getIntExtra(VALUE_GROUP_STEP_LOOP_INDEX, 0),
                stepIndex = getIntExtra(VALUE_GROUP_STEP_STEP_INDEX, 0)
            )
        )
        INDEX_TYPE_END -> TimerIndex.End
        else -> null
    }
}

fun Intent.putTimerIndex(index: TimerIndex?): Intent = apply {
    when (index) {
        is TimerIndex.Start -> {
            putExtra(KEY_INDEX_TYPE, INDEX_TYPE_START)
        }
        is TimerIndex.Step -> {
            putExtra(KEY_INDEX_TYPE, INDEX_TYPE_STEP)
            putExtra(VALUE_LOOP_INDEX, index.loopIndex)
            putExtra(VALUE_STEP_INDEX, index.stepIndex)
        }
        is TimerIndex.Group -> {
            putExtra(KEY_INDEX_TYPE, INDEX_TYPE_GROUP)
            putExtra(VALUE_LOOP_INDEX, index.loopIndex)
            putExtra(VALUE_STEP_INDEX, index.stepIndex)
            val groupStepIndex = index.groupStepIndex
            putExtra(VALUE_GROUP_STEP_LOOP_INDEX, groupStepIndex.loopIndex)
            putExtra(VALUE_GROUP_STEP_STEP_INDEX, groupStepIndex.stepIndex)
        }
        is TimerIndex.End -> {
            putExtra(KEY_INDEX_TYPE, INDEX_TYPE_END)
        }
        else -> Unit
    }
}

fun TimerIndex?.getNiceLoopString(max: Int = 1): String {
    return "%d/%d".format(this?.getLoop(max) ?: 0, max)
}

fun TimerIndex.getLoop(max: Int): Int {
    return when (this) {
        is TimerIndex.Start -> 1
        is TimerIndex.Step -> loopIndex + 1
        is TimerIndex.Group -> loopIndex + 1
        is TimerIndex.End -> max
    }
}
