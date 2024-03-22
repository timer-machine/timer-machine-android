package xyz.aprildown.timer.presentation.timer

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
import xyz.aprildown.timer.domain.TimeUtils.toEpochMilli
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import xyz.aprildown.timer.domain.usecases.folder.FolderSortByRule
import xyz.aprildown.timer.domain.usecases.folder.GetFolders
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.record.GetRecords
import xyz.aprildown.timer.domain.usecases.timer.GetTimerInfo
import xyz.aprildown.timer.presentation.BaseViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val getRecords: GetRecords,
    getFolders: GetFolders,
    folderSortByRule: FolderSortByRule,
    getTimerInfo: GetTimerInfo,

    val preferencesRepository: PreferencesRepository,
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

    private val _minDateMilli: MutableLiveData<Long> = MutableLiveData()
    val minDateMilli: LiveData<Long> = _minDateMilli

    init {
        launch {
            val minDateMilli: Long = getRecords.getMinDateMilli()
            _minDateMilli.value = minDateMilli

            val all = mutableListOf<TimerInfo>()
            val sortBy = folderSortByRule.get()
            for (folder in getFolders().filter { !it.isTrash }) {
                all += getTimerInfo(GetTimerInfo.Params(folder.id, sortBy))
            }
            _allTimerInfo.value = all

            updateStartTime(
                since = preferencesRepository.getInt(PREF_START_SINCE, START_SINCE_LAST_WEEK),
                timerInfo = preferencesRepository.getNullableString(PREF_SELECTED_TIMER_IDS)
                    ?.split(SELECTED_TIMER_IDS_SEPARATOR)
                    ?.mapNotNull { it.toIntOrNull() }
                    ?.let { ids -> all.filter { it.id in ids } }
                    ?.takeIf { it.isNotEmpty() }
                    ?: all
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

            launch {
                val allTimerInfo = _allTimerInfo.value ?: emptyList()
                preferencesRepository.setString(
                    PREF_SELECTED_TIMER_IDS,
                    if (!timerInfo.containsAll(allTimerInfo) ||
                        !allTimerInfo.containsAll(timerInfo)
                    ) {
                        newParams.timerIds.joinToString(
                            separator = SELECTED_TIMER_IDS_SEPARATOR,
                            prefix = "",
                            postfix = ""
                        )
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun updateStartTime(
        since: Int,
        timerInfo: List<TimerInfo> = selectedTimerInfo.value ?: emptyList()
    ) {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val dateTime = LocalDateTime.ofInstant(now, zone)

        val start = when (since) {
            START_SINCE_NOW -> dateTime
            START_SINCE_LAST_WEEK -> dateTime.minusWeeks(1)
            START_SINCE_LAST_MONTH -> dateTime.minusMonths(1)
            START_SINCE_LAST_YEAR -> dateTime.minusYears(1)
            START_SINCE_THE_START -> {
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(
                        minDateMilli.value ?: TimerStampEntity.getMinDateMilli()
                    ),
                    zone
                )
            }
            else -> error("Unsupported since $since")
        }

        updateParams(
            timerInfo = timerInfo,
            startTime = start.toEpochMilli(),
            endTime = now.toEpochMilli()
        )

        launch {
            preferencesRepository.setInt(PREF_START_SINCE, since)
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

    companion object {
        private const val PREF_START_SINCE = "records_start_since"
        const val START_SINCE_NOW = 0
        const val START_SINCE_LAST_WEEK = 1
        const val START_SINCE_LAST_MONTH = 2
        const val START_SINCE_LAST_YEAR = 3
        const val START_SINCE_THE_START = 4

        private const val PREF_SELECTED_TIMER_IDS = "records_selected_timer_ids"
        private const val SELECTED_TIMER_IDS_SEPARATOR = ","
    }
}
