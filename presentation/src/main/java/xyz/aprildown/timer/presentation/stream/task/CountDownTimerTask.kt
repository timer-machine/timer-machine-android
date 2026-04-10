package xyz.aprildown.timer.presentation.stream.task

import com.github.cardinalby.accuratecountdowntimer.AccurateCountDownTimer
import com.github.deweyreed.tools.helper.HandlerHelper
import xyz.aprildown.timer.presentation.stream.StreamState

internal class CountDownTimerTask(master: TaskMaster, countDownTime: Long) : Task(master) {
    private var timer = MyTimer(countDownTime)
    private var millisLeft = countDownTime

    override val currentTime: Long get() = millisLeft

    override fun start() {
        super.start()
        timer.start()
    }

    override fun pause() {
        super.pause()
        timer.cancel()
        timer = MyTimer(millisLeft)
    }

    override fun forceStop() {
        super.forceStop()
        timer.cancel()
    }

    override fun adjust(amount: Long, add: Boolean) {
        val newTime = if (add) millisLeft + amount else amount
        timer.cancel()
        timer = MyTimer(newTime)
        if (taskState.isRunning) {
            timer.start()
        }
    }

    private fun onFinish() {
        taskState = StreamState.RESET
        master.onTaskDone(this)
    }

    private fun onTick(millisUntilFinished: Long) {
        millisLeft = millisUntilFinished
        master.onTick(this, currentTime)
        tick()
    }

    private inner class MyTimer(
        countDownTime: Long,
    ) : AccurateCountDownTimer(countDownTime, 1_000L) {

        init {
            HandlerHelper.runOnUiThread {
                this@CountDownTimerTask.onTick(countDownTime)
            }
        }

        override fun onFinish() {
            HandlerHelper.runOnUiThread {
                this@CountDownTimerTask.onFinish()
            }
        }

        override fun onTick(millisUntilFinished: Long) {
            HandlerHelper.runOnUiThread {
                this@CountDownTimerTask.onTick(millisUntilFinished.round())
            }
        }
    }
}
