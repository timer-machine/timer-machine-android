package xyz.aprildown.timer.presentation.stream

/**
 * timerId will be 0 if the listener is registered with addListener.
 * It will be the real timerId if it's registered with addAllListener.
 */
interface TimerMachineListener {
    fun begin(timerId: Int)
    fun started(timerId: Int, index: TimerIndex)
    fun paused(timerId: Int)
    fun updated(timerId: Int, time: Long)
    fun finished(timerId: Int)
    fun end(timerId: Int, forced: Boolean)
}
