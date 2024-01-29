package xyz.aprildown.timer.presentation.stream

import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.HalfAction
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.entities.VibrationAction
import xyz.aprildown.timer.domain.entities.toBeepAction
import xyz.aprildown.timer.domain.entities.toCountAction
import xyz.aprildown.timer.domain.entities.toFlashlightAction
import xyz.aprildown.timer.domain.entities.toMusicAction
import xyz.aprildown.timer.domain.entities.toNotificationAction
import xyz.aprildown.timer.domain.entities.toScreenAction
import xyz.aprildown.timer.domain.entities.toVibrationAction
import xyz.aprildown.timer.domain.entities.toVoiceAction
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import xyz.aprildown.timer.domain.repositories.TaskerEventTrigger
import xyz.aprildown.timer.domain.usecases.record.AddTimerStamp
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.domain.utils.fireAndForget
import xyz.aprildown.timer.presentation.R
import javax.inject.Inject

class MachinePresenter @Inject constructor(
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val prefRepo: PreferencesRepository,
    private val getTimer: GetTimer,
    private val addTimerStamp: AddTimerStamp,
    private val appTracker: AppTracker,
    private val taskerEventTrigger: TaskerEventTrigger,
) : MachineContract.Presenter, TimerMachine.Listener {

    internal data class TimerMachinePair(val timer: TimerEntity, val machine: TimerMachine)

    override var view: MachineContract.View? = null
    override var isInTheForeground: Boolean = false
    override var currentNotifState: NotifState = NoNotif

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val timers: ArrayMap<Int, TimerMachinePair> = arrayMapOf()

    private val listeners: ArrayMap<Int, MutableList<TimerMachineListener>> = arrayMapOf()
    private val allListeners: MutableList<TimerMachineListener> = mutableListOf()

    /**
     * Service starts
     */
    override fun takeView(view: MachineContract.View) {
        this.view = view
    }

    /**
     * Service down, release all resources
     */
    override fun dropView() {
        stopAll()
        require(timers.isEmpty())

        listeners.clear()
        allListeners.clear()

        view?.run {
            cleanUpWorkArea()
            finish()
        }
        isInTheForeground = false
        view = null
    }

    override fun addListener(timerId: Int, listener: TimerMachineListener) {
        listeners.getOrPut(timerId) { mutableListOf() }.add(listener)
    }

    override fun removeListener(timerId: Int, listener: TimerMachineListener) {
        listeners[timerId]?.remove(listener)
    }

    override fun addAllListener(listener: TimerMachineListener) {
        allListeners.add(listener)
    }

    override fun removeAllListener(listener: TimerMachineListener) {
        allListeners.remove(listener)
    }

    override fun getTimerStateInfo(id: Int): MachineContract.CurrentTimerInfo? {
        return timers[id]?.let { (timer, machine) ->
            MachineContract.CurrentTimerInfo(
                timerEntity = timer,
                state = machine.currentTaskState,
                index = machine.currentIndex,
                time = machine.currentTask?.currentTime ?: 0L,
            )
        }
    }

    override fun startTimer(timerId: Int, index: TimerIndex?) {
        fun TimerMachine.setAndStartWith(timer: TimerEntity) {
            if (index != null && currentIndex != index && timer.isThisIndexValid(index)) {
                toIndex(index)
            }
            start()
        }

        if (timers.containsKey(timerId)) {
            // This timer has been loaded. Simply start it.
            // Possible situations are paused...
            timers[timerId]?.let { (timer, machine) ->
                machine.setAndStartWith(timer)
            }
        } else {
            fireAndForget(mainDispatcher) {
                getTimer.execute(timerId)
                    ?.takeIf { it.folderId != FolderEntity.FOLDER_TRASH }
                    ?.let { timer ->
                        val machine = TimerMachine(timer, this@MachinePresenter)
                        // The only place to adding item to timers
                        timers[timer.id] = TimerMachinePair(timer, machine)
                        machine.setAndStartWith(timer)
                    }
                // repeat(7) {
                //     addTimerStamp.execute(
                //         xyz.aprildown.timer.domain.TestData.getRandomDaysTimerStamp(
                //             timerId = timerId,
                //             from = System.currentTimeMillis() -
                //                     java.util.concurrent.ThreadLocalRandom.current().nextLong(7)
                //                     * 24 * 60 * 60 * 1000
                //         ).copy(id = 0)
                //     )
                // }
                prefRepo.setBoolean(Constants.PREF_HAS_RUNNING_TIMERS, true)
            }
        }
    }

    override fun pauseTimer(timerId: Int) {
        timers[timerId]?.machine?.pause()
    }

    override fun moveTimer(timerId: Int, index: TimerIndex) {
        timers[timerId]?.machine?.toIndex(index)
    }

    override fun decreTimer(timerId: Int) {
        timers[timerId]?.run {
            val current = machine.currentIndex
            if (current == timer.getFirstIndex()) {
                resetTimer(timerId)
            } else {
                val (index, _) =
                    getPrevIndexWithStep(timer.steps, timer.loop, machine.currentIndex)
                moveTimer(timerId, index)
            }
        }
    }

    override fun increTimer(timerId: Int) {
        timers[timerId]?.run {
            val current = machine.currentIndex
            if (current == timer.getLastIndex()) {
                resetTimer(timerId)
            } else {
                val (index, _) =
                    getNextIndexWithStep(timer.steps, timer.loop, machine.currentIndex)
                moveTimer(timerId, index)
            }
        }
    }

    override fun resetTimer(timerId: Int) {
        if (timers.containsKey(timerId)) {
            timers[timerId]?.machine?.stop()
            require(!timers.keys.contains(timerId))
        }
        stopBehaviours()
        stopMachineServiceIfNotRunning()
    }

    override fun adjustAmount(timerId: Int, amount: Long, goBackOnNotifier: Boolean) {
        timers[timerId]?.let { (timer, machine) ->
            if (goBackOnNotifier && amount > 0 &&
                timer.getStep(machine.currentIndex)?.type == StepType.NOTIFIER
            ) {
                decreTimer(timerId)
                if (machine.currentTaskState.isRunning) {
                    machine.to1Minute()
                }
            } else {
                machine.adjust(amount)
            }
        }
    }

    override fun startAll() {
        timers.values.forEach { (_, machine) ->
            if (machine.currentTaskState.isPaused) {
                machine.start()
            }
        }
    }

    override fun pauseAll(): List<Int> {
        val pausedTimerIds = mutableListOf<Int>()
        timers.values.forEach { (timer, machine) ->
            if (machine.currentTaskState.isRunning) {
                pausedTimerIds += timer.id
                machine.pause()
            }
        }
        return pausedTimerIds
    }

    override fun stopAll() {
        timers.keys.map { it }.forEach {
            resetTimer(it)
        }
    }

    override fun scheduleStart(timerId: Int) {
        startTimer(timerId)
    }

    override fun scheduleEnd(timerId: Int) {
        if (timers.containsKey(timerId)) {
            timers[timerId]?.let { (timer, _) ->
                if (timer.endStep != null) {
                    moveTimer(timerId, TimerIndex.End)
                } else {
                    resetTimer(timerId)
                }
            }
        }
        stopMachineServiceIfNotRunning()
    }

    private fun stopBehaviours() {
        view?.run {
            stopMusic()
            stopVibrating()
            closeScreen()
            stopReading()
            disableTone()
            dismissBehaviourNotification()
            toggleFlashlight(null)
        }
    }

    private fun startBehaviours(id: Int, index: TimerIndex) {
        stopBehaviours()
        timers[id]?.let { (timer, _) ->
            timer.getStep(index)?.let { currentStep ->
                val stepBehaviours = currentStep.behaviour
                stepBehaviours.forEach { behavior ->
                    when (behavior.type) {
                        // Handle screen first. This may help priority.
                        BehaviourType.SCREEN -> {
                            val action = behavior.toScreenAction()
                            view?.showScreen(timer, currentStep.label, action.fullScreen)
                        }
                        BehaviourType.VIBRATION -> {
                            val action = behavior.toVibrationAction()
                            if (action.count == 0) {
                                view?.startVibrating(action.vibrationPattern.pattern, true)
                            } else {
                                view?.startVibrating(action.calculateVibratorPattern(), false)
                            }
                        }
                        BehaviourType.BEEP -> {
                            val action = behavior.toBeepAction()
                            view?.enableTone(
                                tone = action.soundIndex,
                                count = action.count,
                                respectOtherSound = action.respectOtherSound
                            )
                        }
                        BehaviourType.COUNT -> {
                            val action = behavior.toCountAction()
                            if (action.beep) {
                                view?.enableTone(
                                    tone = 0,
                                    count = action.times,
                                    respectOtherSound = true,
                                )
                            }
                        }
                        BehaviourType.NOTIFICATION -> {
                            val action = behavior.toNotificationAction()
                            view?.showBehaviourNotification(timer, index, action.duration)
                        }
                        BehaviourType.FLASHLIGHT -> {
                            view?.toggleFlashlight(
                                action = behavior.toFlashlightAction(),
                                duration = if (stepBehaviours.any { it.type == BehaviourType.HALT }) {
                                    // This causes CountDownTimer to overflow, but it works fine.
                                    Long.MAX_VALUE
                                } else {
                                    currentStep.length
                                }
                            )
                        }
                        else -> Unit
                    }
                }
                // Handle voice and music order here
                stepBehaviours.find { it.type == BehaviourType.VOICE }.let { voiceMaybe ->
                    fun BehaviourEntity.playMusic() {
                        val action = toMusicAction()
                        view?.playMusic(action.uri.toUri(), action.loop)
                    }
                    if (voiceMaybe != null) {
                        stepBehaviours.find { it.type == BehaviourType.MUSIC }.let { musicMaybe ->
                            view?.beginReading(
                                content = voiceMaybe.toVoiceAction()
                                    .generateVoiceContent(
                                        timer = timer,
                                        currentStep = currentStep,
                                        index = index,
                                        timeFormatter = object : TimeFormatter {
                                            override fun formatDuration(duration: Long): CharSequence {
                                                return view?.formatDuration(duration) ?: ""
                                            }

                                            override fun formatTime(time: Long): CharSequence {
                                                return view?.formatTime(time) ?: ""
                                            }
                                        }
                                    ),
                                sayMore = false,
                                afterDone = if (musicMaybe != null) {
                                    { musicMaybe.playMusic() }
                                } else {
                                    null
                                }
                            )
                        }
                    } else {
                        stepBehaviours.find { it.type == BehaviourType.MUSIC }
                            ?.playMusic()
                    }
                }
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun timerBeginsAction(id: Int) {
        val timer = timers[id]?.timer
        if (timer == null) {
            val exception = IllegalStateException("Missing timer for timerBeginsAction")
            appTracker.trackError(
                exception,
                message = "target: $id\ntimers: $timers"
            )
            throw exception
        }

        val thisShowsNotif = timer.more.showNotif
        when (currentNotifState) {
            is NoNotif -> {
                isInTheForeground = true
                view?.prepareForWork()
                if (thisShowsNotif) {
                    currentNotifState = SingleTimer
                    view?.run {
                        createTimerNotification(id, timer)
                        toForeground(id)
                    }
                } else {
                    currentNotifState = ForeNotif
                    view?.run {
                        createForegroundNotif()
                        toForeground()
                    }
                }
            }
            is SingleTimer -> {
                currentNotifState = ForeNotif
                view?.run {
                    createForegroundNotif()
                    toForeground()
                    if (thisShowsNotif) {
                        createTimerNotification(id, timer)
                    }
                }
            }
            is ForeNotif -> {
                currentNotifState = ForeNotif
                if (thisShowsNotif) {
                    view?.createTimerNotification(id, timer)
                }
                updateForeNotifIfPossible()
            }
        }
    }

    /**
     * @return if we should update fore notif timer count
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun timerEndsAction(id: Int): Boolean {
        // if (showNotif) left else up
        val timer = timers[id]
        val thisShowsNotif = timer?.timer?.more?.showNotif ?: true

        // Use a coordinate to identify current location
        val timerCount = timers.size
        val showCount = timers.count { it.value.timer.more.showNotif }
        val noShowCount = timerCount - showCount

        var shouldUpdateForeNotif = false

        // require(timerCount > 0)
        // We're going to ignore wrong states and wait users to report them.

        fun updateForeNotifSelf() {
            currentNotifState = ForeNotif
            if (thisShowsNotif) {
                view?.cancelTimerNotification(id)
            }
            shouldUpdateForeNotif = true
        }

        fun findAnotherTimer(): TimerEntity {
            return timers.valueAt(if (timers.indexOfKey(id) == 0) 1 else 0)!!.timer
        }

        when {
            showCount == 0 -> {
                // The first column
                // require(!thisShowsNotif)
                if (noShowCount == 1) {
                    currentNotifState = NoNotif
                    view?.run {
                        cancelForegroundNotif()
                        stopForegroundState()
                    }
                } else {
                    updateForeNotifSelf()
                }
            }
            showCount == 1 -> {
                when {
                    noShowCount == 0 -> {
                        // (1, 0)
                        // require(thisShowsNotif)
                        currentNotifState = NoNotif
                        view?.run {
                            cancelTimerNotification(id)
                            stopForegroundState()
                        }
                    }
                    noShowCount == 1 -> {
                        // (1, 1)
                        if (thisShowsNotif) {
                            updateForeNotifSelf()
                        } else {
                            currentNotifState = SingleTimer
                            val anotherTimer = findAnotherTimer()
                            view?.run {
                                toForeground(anotherTimer.id)
                                cancelForegroundNotif()
                            }
                        }
                    }
                    noShowCount > 1 -> {
                        // (1, 2), (1, 3)...
                        updateForeNotifSelf()
                    }
                }
            }
            showCount == 2 && noShowCount == 0 -> {
                // require(thisShowsNotif)
                currentNotifState = SingleTimer
                val anotherTimer = findAnotherTimer()
                view?.run {
                    toForeground(anotherTimer.id)
                    cancelTimerNotification(id)
                    cancelForegroundNotif()
                }
            }
            else -> {
                // (2, x(x > 0)) && (x, y)
                updateForeNotifSelf()
            }
        }
        return shouldUpdateForeNotif
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun updateForeNotifIfPossible() {
        if (currentNotifState === ForeNotif) {
            val totalTimersCount = timers.size
            view?.updateForegroundNotif(
                totalTimersCount = totalTimersCount,
                pausedTimersCount = timers.count { it.value.machine.currentTaskState.isPaused },
                theOnlyTimerName = if (totalTimersCount == 1) {
                    timers.valueAt(0).timer.name
                } else {
                    null
                }
            )
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun stopMachineServiceIfNotRunning() {
        if (timers.isEmpty()) {
            isInTheForeground = false
            view?.run {
                cleanUpWorkArea()
                finish()
            }
            runBlocking { prefRepo.setBoolean(Constants.PREF_HAS_RUNNING_TIMERS, false) }
        }
    }

    override fun begin(timerId: Int) {
        timerBeginsAction(timerId)

        taskerEventTrigger.timerStart(timerId)

        listeners[timerId]?.forEach { it.begin(0) }
        allListeners.forEach { it.begin(timerId) }
    }

    override fun started(timerId: Int, index: TimerIndex) {
        updateForeNotifIfPossible()
        startBehaviours(timerId, index)

        listeners[timerId]?.forEach { it.started(0, index) }
        allListeners.forEach { it.started(timerId, index) }
    }

    override fun paused(timerId: Int) {
        updateForeNotifIfPossible()
        stopBehaviours()

        listeners[timerId]?.forEach { it.paused(0) }
        allListeners.forEach { it.paused(timerId) }
    }

    override fun updated(timerId: Int, time: Long) {
        listeners[timerId]?.forEach { it.updated(0, time) }
        allListeners.forEach { it.updated(timerId, time) }
    }

    override fun finished(timerId: Int) {
        stopBehaviours()

        listeners[timerId]?.forEach { it.finished(0) }
        allListeners.forEach { it.finished(timerId) }
    }

    override fun end(timerId: Int, forced: Boolean) {
        if (!timers.containsKey(timerId)) {
            appTracker.trackError(
                IllegalStateException("Missing timer for end"),
                message = "target: $timerId\nforced: $forced\ntimers: $timers"
            )
        }

        val shouldUpdateForeNotif = timerEndsAction(timerId)

        val timer = timers.remove(timerId)
        // Update foreNotif with updated timers count
        if (shouldUpdateForeNotif) {
            updateForeNotifIfPossible()
        }

        // If ends normally, stopBehaviours has been called in the [finished],
        // If ends manually, stopBehaviours has been called in the resetTimer.
        if (!forced ||
            (timer != null && timer.machine.currentIndex.isTheLastInTimer(timer.timer))
        ) {
            // Record if the timer finished normally or is stopped manually at the last step.
            fireAndForget(mainDispatcher) {
                addTimerStamp.execute(
                    TimerStampEntity(
                        timerId,
                        timer?.machine?.beginTime ?: (System.currentTimeMillis() - 1)
                    )
                )
            }
        }

        taskerEventTrigger.timerEnd(timerId)

        // toList avoids ConcurrentModificationException.
        listeners[timerId]?.toList()?.forEach { it.end(0, forced) }
        allListeners.toList().forEach { it.end(timerId, forced) }

        val triggerId = timer?.timer?.more?.triggerTimerId
        if (triggerId != null && !forced && triggerId != TimerEntity.NULL_ID) {
            startTimer(triggerId)
        } else {
            stopMachineServiceIfNotRunning()
        }
    }

    override fun beep() {
        view?.playTone()
    }

    override fun notifyHalf(halfOption: Int) {
        view?.run {
            when (halfOption) {
                HalfAction.OPTION_VOICE ->
                    beginReading(contentRes = R.string.half_content, sayMore = false)
                HalfAction.OPTION_MUSIC ->
                    playMusic(Uri.EMPTY, loop = false)
                HalfAction.OPTION_VIBRATION ->
                    startVibrating(VibrationAction.VibrationPattern.Normal().twicePattern, false)
            }
        }
    }

    override fun countRead(content: String) {
        view?.beginReading(content = content, sayMore = true)
    }
}
