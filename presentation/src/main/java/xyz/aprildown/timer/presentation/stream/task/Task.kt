package xyz.aprildown.timer.presentation.stream.task

import androidx.annotation.CallSuper
import xyz.aprildown.timer.presentation.stream.StreamState

internal abstract class Task(protected val master: TaskMaster) {

    var taskState: StreamState =
        StreamState.RESET
        internal set
    open val currentTime: Long = 0L

    @CallSuper
    open fun start() {
        taskState = StreamState.RUNNING
    }

    @CallSuper
    open fun pause() {
        taskState = StreamState.PAUSED
    }

    @CallSuper
    open fun forceStop() {
        taskState = StreamState.RESET
    }

    /**
     * @param add True to add amount to the current time. False to set current time to amount
     */
    abstract fun adjust(amount: Long, add: Boolean)
}
