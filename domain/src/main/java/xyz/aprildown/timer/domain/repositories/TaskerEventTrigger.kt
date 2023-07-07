package xyz.aprildown.timer.domain.repositories

interface TaskerEventTrigger {
    fun timerStart(id: Int)
    fun timerEnd(id: Int)
}
