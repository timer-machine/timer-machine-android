package xyz.aprildown.timer.presentation.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.deweyreed.tools.arch.Event
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.TimerMoreEntity
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.notifier.GetNotifier
import xyz.aprildown.timer.domain.usecases.notifier.SaveNotifier
import xyz.aprildown.timer.domain.usecases.timer.AddTimer
import xyz.aprildown.timer.domain.usecases.timer.DeleteTimer
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import xyz.aprildown.timer.domain.usecases.timer.SaveTimer
import xyz.aprildown.timer.domain.usecases.timer.ShareTimer
import xyz.aprildown.timer.presentation.BaseViewModel
import xyz.aprildown.timer.presentation.R
import xyz.aprildown.timer.presentation.di.ViewModelModule
import javax.inject.Inject
import javax.inject.Named

/**
 * Instead of putting all work to ViewModel,
 * EditTimerActivity takes care of adding and editing steps
 * and send final results to ViewModel when the user saves the timer.
 * In short, no middle states, only final results
 */
@HiltViewModel
class EditViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val addTimer: AddTimer,
    private val saveTimer: SaveTimer,
    private val deleteTimer: Lazy<DeleteTimer>,
    private val getTimer: GetTimer,
    private val findTimerInfo: FindTimerInfo,
    private val getNotifier: GetNotifier,
    private val saveNotifier: SaveNotifier,
    @Named(ViewModelModule.DEFAULT_TIMER_NAME) private val defaultName: String,

    private val shareTimer: ShareTimer,
) : BaseViewModel(mainDispatcher) {

    private var timerHash: Int = 0

    // Updated instantly
    val name = MutableLiveData<String>()
    val loop = MutableLiveData<Int>()
    val more = MutableLiveData<TimerMoreEntity>()

    // Only for passing data
    private val _stepsEvent = MutableLiveData<Event<List<StepEntity>>>()
    val stepsEvent: LiveData<Event<List<StepEntity>>> = _stepsEvent
    private val _startEndEvent = MutableLiveData<Event<Pair<StepEntity.Step?, StepEntity.Step?>>>()
    val startEndEvent: LiveData<Event<Pair<StepEntity.Step?, StepEntity.Step?>>> = _startEndEvent

    // A mutable notifier step
    private var isNotifierChanged: Boolean = false
    var notifier = StepEntity.Step("", 10_000L, listOf(), StepType.NOTIFIER)
        set(value) {
            field = value
            isNotifierChanged = true
        }

    private val _message = MutableLiveData<Event<Int>>()
    val message: LiveData<Event<Int>> = _message

    private val _updatedEvent = MutableLiveData<Int>()
    val updatedEvent: LiveData<Int> = _updatedEvent

    private val _timerInfoEvent = MutableLiveData<Event<TimerInfo>>()
    val timerInfoEvent: LiveData<Event<TimerInfo>> = _timerInfoEvent

    var id: Int = TimerEntity.NEW_ID
        private set
    private var folderId: Long = FolderEntity.FOLDER_DEFAULT
    var isNewTimer = true
        private set
    var oldTimer: TimerEntity? = null
        private set

    private val _shareStringEvent = MutableLiveData<Event<Fruit<String>>>()
    val shareStringEvent: LiveData<Event<Fruit<String>>> = _shareStringEvent

    fun init(timerId: Int, folderId: Long) {
        id = timerId
        isNewTimer = id == TimerEntity.NEW_ID
        this.folderId = folderId
    }

    fun loadTimerData(): Job? = when {
        !isNewTimer -> launch {
            val savedTimer = getTimer(id)
            oldTimer = savedTimer
            if (savedTimer != null) {
                timerHash = savedTimer.hashCode()
                name.value = savedTimer.name
                loop.value = savedTimer.loop
                _stepsEvent.value = Event(savedTimer.steps)
                _startEndEvent.value = Event(Pair(savedTimer.startStep, savedTimer.endStep))
                more.value = savedTimer.more
                folderId = savedTimer.folderId
            }
        }
        else -> {
            // Default
            name.value = defaultName
            loop.value = defaultLoop
            null
        }
    }

    fun loadSampleTimer(timer: TimerEntity) {
        name.value = timer.name
        loop.value = timer.loop
        _stepsEvent.value = Event(timer.steps)
        _startEndEvent.value = Event(Pair(timer.startStep, timer.endStep))
        more.value = timer.more
    }

    fun requestTimerInfoByTimerId(timerId: Int) = launch {
        _timerInfoEvent.value =
            Event(findTimerInfo(timerId) ?: TimerInfo(id = TimerEntity.NULL_ID, name = ""))
    }

    fun saveTimer(
        newSteps: List<StepEntity>,
        start: StepEntity.Step? = null,
        end: StepEntity.Step? = null
    ): Job? {
        if (!validateData(newSteps)) return null

        val newName: String = name.value ?: ""
        val newLoop: Int = loop.value ?: 0
        val more = more.value ?: TimerMoreEntity()
        return launch {
            if (isNewTimer || id == TimerEntity.NEW_ID) {
                addTimer(
                    TimerEntity(
                        id = TimerEntity.NEW_ID,
                        name = newName,
                        loop = newLoop,
                        steps = newSteps,
                        startStep = start,
                        endStep = end,
                        more = more,
                        folderId = folderId
                    )
                )
                _updatedEvent.value = UPDATE_CREATE
            } else {
                saveTimer(
                    TimerEntity(
                        id = id,
                        name = newName,
                        loop = newLoop,
                        steps = newSteps,
                        startStep = start,
                        endStep = end,
                        more = more,
                        folderId = folderId
                    )
                )
                _updatedEvent.value = UPDATE_UPDATE
            }
        }
    }

    /**
     * @return If the data is valid.
     */
    private fun validateData(newSteps: List<StepEntity>): Boolean {
        if (name.value.isNullOrBlank()) {
            _message.value = Event(R.string.edit_wrong_empty_name)
            return false
        }
        val newLoop: Int? = loop.value
        if (newLoop == null || newLoop <= 0) {
            _message.value = Event(R.string.edit_wrong_negative_loop)
            return false
        }
        if (newSteps.isEmpty()) {
            _message.value = Event(R.string.edit_wrong_empty_steps)
            return false
        }
        return true
    }

    fun deleteTimer() = launch {
        deleteTimer.get().invoke(id)
        _updatedEvent.value = UPDATE_DELETE
    }

    fun isTimerRemainingSame(
        newSteps: List<StepEntity>,
        start: StepEntity.Step? = null,
        end: StepEntity.Step? = null
    ): Boolean {
        return if (isNewTimer) {
            name.value == defaultName && loop.value == defaultLoop && more.value == null &&
                start == null && end == null && newSteps.size == 1 &&
                newSteps[0].let {
                    it is StepEntity.Step && it.length == 60_000L &&
                        it.behaviour.isEmpty() && it.type == StepType.NORMAL
                }
        } else {
            TimerEntity(
                id, name.value ?: "", loop.value ?: 0,
                newSteps, start, end, more.value ?: TimerMoreEntity(), folderId
            ).hashCode() == timerHash
        }
    }

    fun loadStoredNotifierStep() = launch {
        notifier = getNotifier()
        isNotifierChanged = false
    }

    fun saveNotifierStep(): Job = launch(NonCancellable) {
        if (isNotifierChanged) {
            saveNotifier(notifier)
        }
    }

    fun generateTimerString(
        newSteps: List<StepEntity>,
        start: StepEntity.Step? = null,
        end: StepEntity.Step? = null
    ) {
        if (!validateData(newSteps)) return

        launch {
            _shareStringEvent.value = Event(
                shareTimer.shareAsString(
                    listOf(
                        TimerEntity(
                            id = TimerEntity.NEW_ID,
                            name = name.value ?: "",
                            loop = loop.value ?: defaultLoop,
                            steps = newSteps,
                            startStep = start,
                            endStep = end,
                        )
                    )
                )
            )
        }
    }

    fun populateWithString(string: String) {
        viewModelScope.launch {
            val fruit = shareTimer.receiveFromString(string)
            if (fruit is Fruit.Ripe) {
                val timer = fruit.data?.timers?.firstOrNull()
                if (timer != null) {
                    name.value = timer.name
                    loop.value = timer.loop
                    _stepsEvent.value = Event(timer.steps)
                    _startEndEvent.value = Event(Pair(timer.startStep, timer.endStep))
                }
            }
        }
    }

    companion object {
        const val defaultLoop = 3

        const val UPDATE_CREATE = 0
        const val UPDATE_UPDATE = 1
        const val UPDATE_DELETE = 2
    }
}
