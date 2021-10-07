package xyz.aprildown.timer.app.timer.one

import android.widget.TextView
import androidx.databinding.BindingAdapter
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.one.step.StepListView
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getNiceLoopString
import xyz.aprildown.tools.helper.setTextIfChanged

@BindingAdapter("oneTime")
internal fun TextView.setTimeInMilli(time: Long?) {
    text = (time ?: 0L).produceTime()
}

@BindingAdapter("oneLoop", "oneTotalLoop")
internal fun TextView.setOneLoop(index: TimerIndex?, totalLoop: Int) {
    setTextIfChanged(index.getNiceLoopString(totalLoop))
}

// region OneFragment

@BindingAdapter("oneTimer")
internal fun StepListView.setOneTimer(timer: TimerEntity?) {
    if (timer != null) {
        setTimer(timer)
    }
}

@BindingAdapter("oneStepIndex")
internal fun StepListView.setOneStepIndex(index: TimerIndex?) {
    if (index != null) {
        toIndex(index)
    }
}

@BindingAdapter("oneFabState")
internal fun FiveActionsView.setOneFabState(state: StreamState?) {
    changeState(
        if (state?.isRunning == true) {
            FiveActionsView.STATE_PAUSE
        } else {
            FiveActionsView.STATE_PLAY
        }
    )
}

// endregion OneFragment
