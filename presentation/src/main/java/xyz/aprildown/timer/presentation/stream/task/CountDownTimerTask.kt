package xyz.aprildown.timer.presentation.stream.task

import android.os.CountDownTimer
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.tools.helper.HandlerHelper

internal class CountDownTimerTask(master: TaskMaster, countDownTime: Long) : Task(master) {

    // We place tickListeners before timer because MyTimer will onTick in the init
    private val tickListeners = mutableListOf<TickListener>()

    private var timer = MyTimer(countDownTime)
    private var millisLeft = countDownTime

    override val currentTime: Long get() = millisLeft

    fun addTickListener(listener: TickListener) {
        tickListeners.add(listener)
    }

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
        master.onTick(this, millisUntilFinished)
        tickListeners.forEach { it.onNewTime(millisUntilFinished) }
    }

    private inner class MyTimer(countDownTime: Long) : CountDownTimer(countDownTime, 1_000L) {

        private var remainingTime = countDownTime

        init {
            HandlerHelper.runOnUiThread {
                this@CountDownTimerTask.onTick(remainingTime)
            }
        }

        override fun onFinish() {
            HandlerHelper.runOnUiThread {
                this@CountDownTimerTask.onFinish()
            }
        }

        override fun onTick(millisUntilFinished: Long) {
            HandlerHelper.runOnUiThread {
                this@CountDownTimerTask.onTick(remainingTime)
                remainingTime -= 1_000L
            }
        }
    }
}
