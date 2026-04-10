package xyz.aprildown.timer.presentation.stream.task

import com.github.cardinalby.accuratecountdowntimer.AccurateCountDownTimer
import com.github.deweyreed.tools.helper.HandlerHelper
import xyz.aprildown.timer.presentation.stream.StreamState

/**
 * You must call [TaskManager.interfere] to move on.
 */
internal class StopwatchTask(master: TaskMaster) : Task(master) {

    private var timer = MyTimer()
    private var millisPassedBase = 0L
    private var millisPassedCurrent = 0L

    override val currentTime: Long get() = millisPassedBase + millisPassedCurrent

    override fun start() {
        super.start()
        timer.start()
    }

    override fun pause() {
        super.pause()
        timer.cancel()
        millisPassedBase += millisPassedCurrent
        millisPassedCurrent = 0L
        timer = MyTimer()
    }

    override fun forceStop() {
        super.forceStop()
        timer.cancel()
    }

    override fun adjust(amount: Long, add: Boolean) {
        timer.cancel()
        millisPassedBase = if (add) millisPassedBase + millisPassedCurrent + amount else amount
        millisPassedCurrent = 0L
        timer = MyTimer()
        if (taskState.isRunning) {
            timer.start()
        }
    }

    private fun onFinish() {
        taskState = StreamState.RESET
        master.onTaskDone(this)
    }

    private fun onTick(millisPassed: Long) {
        millisPassedCurrent = millisPassed
        master.onTick(this, currentTime)
        tick()
    }

    private inner class MyTimer : AccurateCountDownTimer(DURATION, 1_000L) {

        init {
            HandlerHelper.runOnUiThread {
                this@StopwatchTask.onTick(0L)
            }
        }

        override fun onFinish() {
            HandlerHelper.runOnUiThread {
                this@StopwatchTask.onFinish()
            }
        }

        override fun onTick(millisUntilFinished: Long) {
            HandlerHelper.runOnUiThread {
                this@StopwatchTask.onTick((DURATION - millisUntilFinished).round())
            }
        }
    }
}

private const val DURATION = Long.MAX_VALUE
