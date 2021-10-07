package xyz.aprildown.timer.presentation.timer

import android.text.format.DateUtils
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.TimeUtils
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.usecases.folder.FolderSortByRule
import xyz.aprildown.timer.domain.usecases.folder.GetFolders
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.record.GetRecords
import xyz.aprildown.timer.domain.usecases.timer.GetTimerInfo
import xyz.aprildown.timer.presentation.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val getRecords: GetRecords,
    getFolders: GetFolders,
    folderSortByRule: FolderSortByRule,
    getTimerInfo: GetTimerInfo
) : BaseViewModel(mainDispatcher) {

    private val _allTimerInfo = MutableLiveData<List<TimerInfo>>()
    val allTimerInfo: LiveData<List<TimerInfo>> = _allTimerInfo

    private val _selectedTimerInfo = MutableLiveData<List<TimerInfo>>()
    val selectedTimerInfo: LiveData<List<TimerInfo>> = _selectedTimerInfo

    private val _startTime = MutableLiveData<Long>()
    val startTime: LiveData<Long> = _startTime

    private val _endTime = MutableLiveData<Long>()
    val endTime: LiveData<Long> = _endTime

    private val _params = MutableLiveData<GetRecords.Params>()
    val params: LiveData<GetRecords.Params> = _params
    val overview: LiveData<GetRecords.Signal<GetRecords.OverviewResult>> =
        params.switchMap {
            getRecords.calculateOverviewRecords(it)
        }
    val timeline: LiveData<GetRecords.Signal<GetRecords.TimelineResult>> =
        params.switchMap {
            getRecords.calculateTimeline(it)
        }

    private val _minDateMilli = MutableLiveData<Long>()
    val minDateMilli: LiveData<Long> = _minDateMilli

    init {
        launch {
            _minDateMilli.value = getRecords.getMinDateMilli()

            val all = mutableListOf<TimerInfo>()
            val sortBy = folderSortByRule.get()
            for (folder in getFolders().filter { !it.isTrash }) {
                all += getTimerInfo(GetTimerInfo.Params(folder.id, sortBy))
            }
            _allTimerInfo.value = all
            val now = System.currentTimeMillis()
            updateParams(
                timerInfo = all,
                startTime = now - DateUtils.WEEK_IN_MILLIS,
                endTime = now
            )
        }
    }

    fun queryTimerName(timerId: Int): String? {
        return allTimerInfo.value?.find { it.id == timerId }?.name
    }

    fun updateParams(
        timerInfo: List<TimerInfo> = selectedTimerInfo.value ?: emptyList(),
        startTime: Long = this.startTime.value ?: 0L,
        endTime: Long = this.endTime.value ?: System.currentTimeMillis()
    ) {
        val startTimeOfTheDay = TimeUtils.getDayStart(startTime)
        val endTimeOfTheDay = TimeUtils.getDayEnd(endTime)
        val newParams = GetRecords.Params(
            timerIds = timerInfo.map { it.id },
            startTime = startTimeOfTheDay,
            endTime = endTimeOfTheDay
        )
        if (newParams != _params.value) {
            _params.value = newParams
            _selectedTimerInfo.value = timerInfo
            _startTime.value = startTimeOfTheDay
            _endTime.value = endTimeOfTheDay
        }
    }

    // region Calendar

    val calendarTimeSpan = MutableLiveData<Pair<Long, Long>>().apply {
        val now = System.currentTimeMillis()
        value = TimeUtils.getMonthStart(now) to TimeUtils.getMonthEnd(now)
    }

    val calendarEvents: LiveData<GetRecords.Signal<ArrayMap<Long, Int>>> =
        selectedTimerInfo.asFlow().combine(calendarTimeSpan.asFlow()) { info, span ->
            info to span
        }.asLiveData()
            .distinctUntilChanged()
            .switchMap { pair ->
                val calendarTimeSpan = pair.second
                getRecords.calculateCalendarEvents(
                    GetRecords.Params(
                        timerIds = pair.first.map { it.id },
                        startTime = calendarTimeSpan.first,
                        endTime = calendarTimeSpan.second
                    )
                )
            }

    val calendarSelectedDate = MutableLiveData(
        TimeUtils.getDayStart(System.currentTimeMillis())
    )

    val calendarSelectedDateEvents: LiveData<GetRecords.Signal<List<TimerStampEntity>>> =
        selectedTimerInfo.asFlow().combine(calendarSelectedDate.asFlow()) { info, date ->
            info to date
        }.asLiveData()
            .distinctUntilChanged()
            .switchMap { pair ->
                getRecords.calculateDayEvents(
                    GetRecords.DayParams(
                        timerIds = pair.first.map { it.id },
                        timePoint = pair.second
                    )
                )
            }

    // endregion Calendar
}
