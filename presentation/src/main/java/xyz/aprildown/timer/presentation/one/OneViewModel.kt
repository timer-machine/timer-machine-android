package xyz.aprildown.timer.presentation.one

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import xyz.aprildown.timer.domain.usecases.timer.SaveTimer
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.presentation.BaseViewModel
import xyz.aprildown.timer.presentation.R
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.TimerMachineListener
import xyz.aprildown.timer.presentation.stream.getFirstIndex
import xyz.aprildown.timer.presentation.stream.getLastIndex
import xyz.aprildown.timer.presentation.stream.getNextIndexWithStep
import xyz.aprildown.timer.presentation.stream.getPrevIndexWithStep
import xyz.aprildown.timer.presentation.stream.getStep
import xyz.aprildown.timer.presentation.stream.getTimeBeforeIndex
import xyz.aprildown.timer.presentation.stream.getTotalTime
import xyz.aprildown.tools.arch.Event
import javax.inject.Inject

@HiltViewModel
class OneViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val getTimer: GetTimer,
    private val saveTimer: SaveTimer,
    private val findTimerInfo: FindTimerInfo,
    val streamMachineIntentProvider: StreamMachineIntentProvider,
    private val appTracker: AppTracker,
) : BaseViewModel(mainDispatcher), TimerMachineListener {

    private var timerId: Int = TimerEntity.NULL_ID
    private var theFirstIndex: TimerIndex = TimerIndex.Start

    val timer = MutableLiveData<TimerEntity>()

    val timerCurrentState = MutableLiveData(StreamState.RESET)
    val timerCurrentTime = MutableLiveData(0L)
    val timerCurrentIndex = MutableLiveData<TimerIndex>()

    // Caches

    var timerTotalTime: Long = 0L
        private set
    var timerStepTime: Long = 0L
        private set
    private var elapsedBaseTime: Long = 0L
    private var elapsedCurrentTime = MutableLiveData<Long>()
    val timerElapsedTime: LiveData<Long?> = elapsedCurrentTime.map {
        if (it == null) null else it + elapsedBaseTime
    }

    // UI is unlocked by default
    var uiLocked = MutableLiveData(false)

    val messageEvent = MutableLiveData<Event<Int>>()
    private val _editTimerEvent = MutableLiveData<Event<Int>>()
    val editTimerEvent: LiveData<Event<Int>> = _editTimerEvent
    private val _intentEvent = MutableLiveData<Event<Intent>>()
    val intentEvent: LiveData<Event<Intent>> = _intentEvent
    private val _finishEvent = MutableLiveData<Event<Unit>>()
    val finishEvent: LiveData<Event<Unit>> = _finishEvent

    private var presenter: MachineContract.Presenter? = null

    fun setTimerId(newTimerId: Int) {
        timerId = newTimerId
    }

    fun getBindIntent(): Intent = streamMachineIntentProvider.bindIntent()

    fun setPresenter(newPresenter: MachineContract.Presenter) {
        presenter = newPresenter
        attachPresenter()
    }

    private fun attachPresenter() {
        presenter?.addListener(timerId, this)
        loadTimer()
    }

    private fun loadTimer() {
        val timerInfo = presenter?.getTimerStateInfo(timerId)
        if (timerInfo == null) {
            launch {
                getTimer.execute(timerId)?.let { timer ->
                    val currentTimerInfo = presenter?.getTimerStateInfo(timerId)
                    if (currentTimerInfo != null) {
                        currentTimerInfo.run {
                            setTimerItem(timerEntity, state, index, time)
                        }
                    } else {
                        val firstIndex = timer.getFirstIndex()
                        setTimerItem(
                            timer,
                            StreamState.RESET,
                            firstIndex,
                            timer.getStep(firstIndex)?.length ?: 0
                        )
                    }
                }
            }
        } else {
            timerInfo.run {
                setTimerItem(timerEntity, state, index, time)
            }
        }
    }

    fun onStartPause() {
        if (uiLocked.value == true) {
            showUiLockedMsg()
            return
        }
        if (timer.value?.folderId == FolderEntity.FOLDER_TRASH) {
            messageEvent.value = Event(R.string.one_unable_to_start_timers_in_the_trash)
            return
        }
        val state = timerCurrentState.value ?: return
        if (state.isReset || state.isPaused) {
            _intentEvent.value = Event(
                streamMachineIntentProvider.startIntent(
                    timerId, timerCurrentIndex.value
                )
            )
        } else if (state.isRunning) {
            _intentEvent.value = Event(streamMachineIntentProvider.pauseIntent(timerId))
        }
    }

    fun onReset() {
        if (uiLocked.value == true) {
            showUiLockedMsg()
            return
        }
        if (timerCurrentState.value != StreamState.RESET) {
            _intentEvent.value = Event(streamMachineIntentProvider.resetIntent(timerId))
        }
    }

    fun tweakTime(amount: Long = 60_000L) {
        if (uiLocked.value == true) {
            showUiLockedMsg()
            return
        }
        if (timerCurrentState.value != StreamState.RESET) {
            _intentEvent.value =
                Event(streamMachineIntentProvider.adjustTimeIntent(timerId, amount))
        }
    }

    fun onMove(offset: Int) {
        if (uiLocked.value == true) {
            showUiLockedMsg()
            return
        }
        val state = timerCurrentState.value ?: return
        if (state.isReset || state.isPaused) {
            val timer = timer.value ?: return
            val (index, step) = when (offset) {
                1 -> getNextIndexWithStep(
                    timer.steps,
                    timer.loop,
                    timerCurrentIndex.value ?: theFirstIndex,
                    timer.getLastIndex()
                )
                -1 -> getPrevIndexWithStep(
                    timer.steps,
                    timer.loop,
                    timerCurrentIndex.value ?: theFirstIndex,
                    theFirstIndex
                )
                else -> throw IllegalStateException("Offset $offset is not supported yet")
            }

            timerCurrentTime.value = step?.length ?: 0L
            timerCurrentIndex.value = index
        } else {
            if (offset == 1) {
                _intentEvent.value = Event(streamMachineIntentProvider.increIntent(timerId))
            } else if (offset == -1) {
                _intentEvent.value = Event(streamMachineIntentProvider.decreIntent(timerId))
            }
        }
    }

    fun onJump(newIndex: TimerIndex) {
        if (uiLocked.value == true) {
            showUiLockedMsg()
            return
        }
        // We have to getStep to validate it.
        val step = timer.value?.getStep(newIndex) ?: return
        val state = timerCurrentState.value ?: return
        if (state.isReset || state.isPaused) {
            timerCurrentTime.value = step.length
            timerCurrentIndex.value = newIndex
        } else {
            _intentEvent.value = Event(streamMachineIntentProvider.moveIntent(timerId, newIndex))
        }
    }

    fun onEdit() {
        if (uiLocked.value == true) {
            showUiLockedMsg()
            return
        }
        if (timerCurrentState.value != StreamState.RESET) {
            onReset()
        }
        _editTimerEvent.value = Event(timerId)
    }

    fun updateStep(index: TimerIndex, newStep: StepEntity.Step) = launch {
        if (uiLocked.value == true) {
            showUiLockedMsg()
            return@launch
        }
        if (timerCurrentState.value != StreamState.RESET) {
            onReset()
            timerCurrentIndex.value = index
        }
        val oldTimer = timer.value ?: return@launch
        val newTimer = when (index) {
            TimerIndex.Start -> oldTimer.copy(startStep = newStep)
            TimerIndex.End -> oldTimer.copy(endStep = newStep)
            is TimerIndex.Step -> {
                val newSteps = oldTimer.steps.toMutableList()
                newSteps[index.stepIndex] = newStep
                oldTimer.copy(steps = newSteps)
            }
            is TimerIndex.Group -> {
                val newSteps = oldTimer.steps.toMutableList()
                val newGroup = newSteps[index.stepIndex] as StepEntity.Group
                val newGroupSteps = newGroup.steps.toMutableList()
                newGroupSteps[index.groupStepIndex.stepIndex] = newStep
                newSteps[index.stepIndex] = newGroup.copy(steps = newGroupSteps)
                oldTimer.copy(steps = newSteps)
            }
        }
        saveTimer.execute(newTimer)

        setTimerItem(newTimer, StreamState.RESET, index, newTimer.getStep(index)?.length ?: 0)
    }

    private fun showUiLockedMsg() {
        messageEvent.value = Event(R.string.one_ui_locked)
    }

    private fun setTimerItem(
        timerItem: TimerEntity,
        state: StreamState,
        index: TimerIndex,
        time: Long
    ) {
        timer.value = timerItem
        timerTotalTime = timerItem.getTotalTime()
        theFirstIndex = timerItem.getFirstIndex()

        timerCurrentState.value = state
        timerCurrentTime.value = time
        timerCurrentIndex.value = index

        timerStepTime = timerItem.getStep(index)?.length ?: 0L
        elapsedBaseTime = timerItem.getTimeBeforeIndex(index)
        elapsedCurrentTime.value = 0L
    }

    private fun detachListener() {
        presenter?.removeListener(timerId, this)
    }

    fun dropPresenter() {
        detachListener()
        presenter = null
    }

    //
    // Timer Callbacks
    //

    override fun begin(timerId: Int) = Unit

    override fun started(timerId: Int, index: TimerIndex) {
        timerCurrentState.value = StreamState.RUNNING
        timerCurrentIndex.value = index

        timerStepTime = timer.value?.getStep(index)?.length ?: 0L
        elapsedBaseTime = timer.value?.getTimeBeforeIndex(index, appTracker) ?: 0L
        elapsedCurrentTime.value = 0L
    }

    override fun paused(timerId: Int) {
        timerCurrentState.value = StreamState.PAUSED
    }

    override fun updated(timerId: Int, time: Long) {
        elapsedCurrentTime.value = timerStepTime - time
        timerCurrentTime.value = time
    }

    override fun finished(timerId: Int) = Unit

    override fun end(timerId: Int, forced: Boolean) {
        timerCurrentState.value = StreamState.RESET
        timerCurrentTime.value = 0L
        timerCurrentIndex.value = theFirstIndex

        if (!forced) {
            val timer = timer.value ?: return
            launch {
                val nextId = timer.more.triggerTimerId
                if (nextId != TimerEntity.NULL_ID &&
                    findTimerInfo(nextId).let {
                        it != null && it.folderId != FolderEntity.FOLDER_TRASH
                    }
                ) {
                    detachListener()
                    this@OneViewModel.timerId = nextId
                    attachPresenter()
                } else {
                    _finishEvent.value = Event(Unit)
                }
            }
        }

        timerStepTime = 0L
        elapsedBaseTime = 0L
        elapsedCurrentTime.value = 0L
    }
}
