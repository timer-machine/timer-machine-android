package xyz.aprildown.timer.presentation.stream.task

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * You must call [TaskManager.interfere] to move on.
 */
internal class StopwatchTask(master: TaskMaster) : Task(master) {

    private val tickListeners = mutableListOf<TickListener>()

    private val handler = Handler(Looper.getMainLooper())
    private var currentStartTime = 0L
    private var baseElapsedTime = 0L
    private var currentElapsedTime = 0L

    override val currentTime: Long get() = currentElapsedTime + baseElapsedTime

    override fun start() {
        super.start()
        currentStartTime = SystemClock.elapsedRealtime()
        currentElapsedTime = 0
        handler.post(TickRunnable())
    }

    override fun pause() {
        super.pause()
        handler.removeCallbacksAndMessages(null)
        baseElapsedTime += currentElapsedTime
        currentElapsedTime = 0
    }

    override fun forceStop() {
        super.forceStop()
        handler.removeCallbacksAndMessages(null)
    }

    override fun adjust(amount: Long, add: Boolean) {
        handler.removeCallbacksAndMessages(null)
        baseElapsedTime += if (add) currentElapsedTime + amount else amount
        currentElapsedTime = 0
        if (taskState.isRunning) {
            start()
        } else {
            master.onTick(this, currentTime)
        }
    }

    /**
     * newElapsedTime pattern: 26, 1026, 2026, 3026...
     * So the result is 0, 1, 2, 3
     */
    private fun onTick(newElapsedTime: Long) {
        currentElapsedTime = newElapsedTime
        val time = currentTime
        master.onTick(this, time)
        tickListeners.forEach { it.onNewTime(time) }
    }

    fun addTickListener(listener: TickListener) {
        tickListeners.add(listener)
    }

    private inner class TickRunnable : Runnable {
        override fun run() {
            val tickStartTime = SystemClock.elapsedRealtime()

            val newElapsedTime = tickStartTime - currentStartTime
            onTick(newElapsedTime)

            val tickEndTime = SystemClock.elapsedRealtime()
            val consume = tickEndTime - tickStartTime
            handler.postDelayed(this, 1_000L - (consume % 1_000L))
        }
    }
}
