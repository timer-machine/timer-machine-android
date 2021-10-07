package xyz.aprildown.timer.presentation.stream.task

internal interface TickListener {
    /**
     * Don't use newTime as counter. It may tick twice in one second.
     */
    fun onNewTime(newTime: Long)
}
