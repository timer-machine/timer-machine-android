package xyz.aprildown.timer.presentation.stream

import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.toCountAction
import xyz.aprildown.timer.domain.entities.toHalfAction
import xyz.aprildown.timer.presentation.stream.task.CountDownTimerTask
import xyz.aprildown.timer.presentation.stream.task.StopwatchTask
import xyz.aprildown.timer.presentation.stream.task.Task
import xyz.aprildown.timer.presentation.stream.task.TaskManager
import xyz.aprildown.timer.presentation.stream.task.TickListener

internal class TimerMachine(
    private val timer: TimerEntity,
    private val listener: Listener
) : TaskManager() {

    interface Listener {

        /**
         * These are similar to [TimerMachineListener]. Just to encapsulate our implementation.
         */
        fun begin(timerId: Int)

        fun started(timerId: Int, index: TimerIndex)
        fun paused(timerId: Int)
        fun updated(timerId: Int, time: Long)
        fun finished(timerId: Int)
        fun end(timerId: Int, forced: Boolean)

        fun beep()
        fun notifyHalf(halfOption: Int)
        fun countRead(content: String)
    }

    private val timerId = timer.id
    private val theLastIndex: TimerIndex = timer.getLastIndex()

    var currentIndex: TimerIndex = timer.getFirstIndex()
        private set

    // region Custom Actions

    fun toIndex(newIndex: TimerIndex) {
        val step = timer.getStep(newIndex)
        if (step != null) {
            currentIndex = newIndex
            interfere(step.toTask())
        }
    }

    fun adjust(amount: Long) {
        currentTask?.adjust(amount, add = true)
    }

    fun to1Minute() {
        currentTask?.adjust(60_000L, add = false)
    }

    // endregion Custom Actions

    override fun onManagerBegin() {
        listener.begin(timerId)
    }

    override fun onManagerStart(newTask: Task) {
        listener.started(timerId, currentIndex)
    }

    override fun onManagerPaused(pausedTask: Task) {
        listener.paused(timerId)
    }

    override fun onManagerTick(task: Task, time: Long) {
        listener.updated(timerId, time)
    }

    override fun onManagerDone(oldTask: Task) {
        listener.finished(timerId)
    }

    override fun onManagerNoMore() {
        listener.end(timerId, forced = false)
    }

    override fun onManagerStopped() {
        listener.end(timerId, forced = true)
    }

    override fun provideFirstTask(): Task? {
        val firstIndex = timer.getFirstIndex()
        val (_, nextStepAfterNext) = getNextIndexWithStep(
            timer.steps,
            timer.loop,
            firstIndex
        )
        return timer.getStep(firstIndex)?.toTask(
            useTtsNextStep = nextStepAfterNext?.behaviour?.any { it.useTts() } == true
        )
    }

    override fun provideNextTask(): Task? {
        if (currentIndex == theLastIndex) return null

        var nextIndex: TimerIndex = currentIndex
        do {
            nextIndex = getNextIndexWithStep(timer.steps, timer.loop, nextIndex).first
            val skip = timer.shouldSkip(nextIndex)
            val isLast = nextIndex == theLastIndex
            when {
                skip && isLast -> return null
                skip -> continue
                else -> break
            }
        } while (true)

        val (_, nextStepAfterNext) =
            getNextIndexWithStep(timer.steps, timer.loop, nextIndex)

        currentIndex = nextIndex
        return timer.getStep(nextIndex)?.toTask(
            useTtsNextStep = nextStepAfterNext?.behaviour?.any { it.useTts() } == true
        )
    }

    private fun StepEntity.Step.toTask(useTtsNextStep: Boolean = false): Task {
        val behaviour = behaviour
        val countUp = behaviour.find { it.type == BehaviourType.HALT } != null
        return if (countUp) {
            StopwatchTask(master = this@TimerMachine)
        } else {
            CountDownTimerTask(master = this@TimerMachine, countDownTime = length).apply {
                if (useTtsNextStep) {
                    addTickListener(WarmUpTtsListener(warmUp = { listener.countRead("") }))
                }
            }
        }.apply {
            behaviour.forEach { item ->
                when (item.type) {
                    BehaviourType.BEEP -> {
                        addTickListener(BeepTickListener(beep = listener::beep))
                    }
                    BehaviourType.HALF -> {
                        addTickListener(
                            HalfTickListener(
                                total = length,
                                countUp = countUp,
                                half = { listener.notifyHalf(item.toHalfAction().option) },
                            )
                        )
                    }
                    BehaviourType.COUNT -> {
                        val action = item.toCountAction()
                        addTickListener(
                            CountTickListener(
                                times = action.times,
                                total = length,
                                countUp = countUp,
                                count = if (action.beep) {
                                    {
                                        if (it.isNotBlank()) {
                                            listener.beep()
                                        }
                                    }
                                } else {
                                    { listener.countRead(it) }
                                },
                            )
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    private class BeepTickListener(private val beep: () -> Unit) : TickListener {
        override fun onNewTime(newTime: Long) {
            beep()
        }
    }

    private class HalfTickListener(
        private val total: Long,
        private val countUp: Boolean = false,
        private val half: () -> Unit,
    ) : TickListener {

        private var isNotified = false

        override fun onNewTime(newTime: Long) {
            if (isNotified) return
            val isPassed = if (countUp) {
                newTime > total / 2 - 1000
            } else {
                newTime < total / 2 + 1000
            }
            if (isPassed) {
                isNotified = true
                half()
            }
        }
    }

    private class CountTickListener(
        private var times: Int,
        private val total: Long = 0L,
        private val countUp: Boolean = false,
        private val count: (String) -> Unit,
    ) : TickListener {

        private val warmUpTime = times + 1
        private var isWarmedUp = false

        override fun onNewTime(newTime: Long) {
            val remainingSeconds = if (countUp) {
                (total - newTime) / 1000
            } else {
                newTime / 1000
            }
            if (!isWarmedUp && remainingSeconds <= warmUpTime) {
                isWarmedUp = true
                count("")
            }
            if (remainingSeconds <= times && times > 0) {
                times--
                count((newTime / 1000).toString())
            }
        }
    }

    private class WarmUpTtsListener(private val warmUp: () -> Unit) : TickListener {

        private var isWarmedUp = false

        override fun onNewTime(newTime: Long) {
            if (isWarmedUp) return
            val remainingSeconds = newTime / 1000
            if (remainingSeconds <= 20) {
                warmUp()
                isWarmedUp = true
            }
        }
    }
}
