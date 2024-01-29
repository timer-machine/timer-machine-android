package xyz.aprildown.timer.domain.usecases.record

import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.TimeUtils
import xyz.aprildown.timer.domain.TimeUtils.toEpochMilli
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.absoluteValue

@Reusable
class GetRecords @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: TimerStampRepository
) : CoroutinesUseCase<Unit, ArrayMap<Int, List<TimerStampEntity>>>(dispatcher) {

    sealed class Signal<out Type> {
        data object Processing : Signal<Nothing>()
        data class Result<out Type>(val result: Type) : Signal<Type>()
    }

    override suspend fun create(params: Unit): ArrayMap<Int, List<TimerStampEntity>> {
        throw UnsupportedOperationException()
    }

    suspend fun getMinDateMilli(): Long {
        val stamp =
            repository.getEarliestOne() ?: return LocalDate.now().atStartOfDay().toEpochMilli()

        val time = if (stamp.start > 0L) stamp.start else stamp.end
        if (time <= 0L) return TimerStampEntity.getMinDateMilli()

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay()
            .toEpochMilli()
    }

    data class Params(
        val timerIds: List<Int>,
        val startTime: Long,
        val endTime: Long
    )

    /**
     * null Int represents other timers.
     */
    data class OverviewResult(
        val timeData: ArrayMap<Int?, Entry<Long>>,
        val countData: ArrayMap<Int?, Entry<Int>>
    ) {
        data class Entry<T>(val data: T, val percent: Float)
    }

    fun calculateOverviewRecords(
        params: Params
    ): LiveData<Signal<OverviewResult>> = liveData {
        emit(Signal.Processing)
        emit(
            withContext(dispatcher) {
                Signal.Result(produceOverviewResult(params))
            }
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun produceOverviewResult(params: Params): OverviewResult {
        val records = repository.getRecordsGroupedByTimer(
            timerIds = params.timerIds,
            startTime = params.startTime,
            endTime = params.endTime
        )
        var totalTime = 0L
        var totalCount = 0
        val timeData = arrayMapOf<Int?, OverviewResult.Entry<Long>>()
        val countData = arrayMapOf<Int?, OverviewResult.Entry<Int>>()
        records.forEach { pair ->
            val timerId = pair.key
            val stamps = pair.value
            var timerDuration = 0L
            stamps.forEach {
                val duration = it.duration
                totalTime += duration
                timerDuration += duration
            }
            timeData[timerId] = OverviewResult.Entry(timerDuration, 0f)
            val stampCount = stamps.size
            totalCount += stampCount
            countData[timerId] = OverviewResult.Entry(stampCount, 0f)
        }

        if (totalTime == 0L || totalCount == 0) {
            return OverviewResult(timeData = timeData, countData = countData)
        }

        val smallTimeMap = arrayMapOf<Int, OverviewResult.Entry<Long>>()
        timeData.forEach { pair ->
            val timerId = pair.key
            val entry = pair.value
            val durationPercent =
                (entry.data.toDouble() / totalTime.toDouble()).toFloat()
            if (durationPercent >= 0.01f) {
                timeData[timerId] = entry.copy(percent = durationPercent)
            } else {
                smallTimeMap[timerId] = entry
            }
        }
        if (smallTimeMap.isNotEmpty()) {
            smallTimeMap.forEach {
                timeData.remove(it.key)
            }
            val values = smallTimeMap.values
            val smallTotalDuration = values.fold(0L) { acc, timeEntry ->
                acc + timeEntry.data
            }
            timeData[null] = OverviewResult.Entry(
                data = smallTotalDuration,
                percent = (smallTotalDuration.toDouble() / totalTime.toDouble()).toFloat()
            )
        }

        val smallCountMap = arrayMapOf<Int?, OverviewResult.Entry<Int>>()
        countData.forEach { pair ->
            val timerId = pair.key
            val entry = pair.value
            val countPercent = entry.data.toFloat() / totalCount.toFloat()
            if (countPercent >= 0.01f) {
                countData[timerId] = entry.copy(percent = countPercent)
            } else {
                smallCountMap[timerId] = entry
            }
        }
        if (smallCountMap.isNotEmpty()) {
            smallCountMap.forEach {
                countData.remove(it.key)
            }
            val values = smallCountMap.values
            val smallTotalCount = values.fold(0) { acc, countEntry ->
                acc + countEntry.data
            }
            countData[null] = OverviewResult.Entry(
                data = smallTotalCount,
                percent = smallTotalCount.toFloat() / totalCount.toFloat()
            )
        }

        return OverviewResult(timeData = timeData, countData = countData)
    }

    data class TimelineEvent(
        val timePoint: Long,
        val duration: Long,
        val count: Int
    )

    data class TimelineResult(
        val mode: Int,
        val events: List<TimelineEvent>
    ) {
        companion object {
            const val MODE_ONE_DAY = 0
            const val MODE_DAYS = 1
        }
    }

    fun calculateTimeline(
        params: Params
    ): LiveData<Signal<TimelineResult>> =
        liveData {
            emit(Signal.Processing)
            emit(
                withContext(dispatcher) {
                    Signal.Result(produceTimelineResult(params))
                }
            )
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun produceTimelineResult(params: Params): TimelineResult {
        val result = mutableListOf<TimelineEvent>()
        val globalStartTime = TimeUtils.getDayStart(params.startTime)
        val globalEndTime = TimeUtils.getDayEnd(params.endTime)

        val mode: Int
        val step: Long
        val endCalculator: (Long) -> Long
        if ((globalEndTime - globalStartTime).absoluteValue < DateUtils.DAY_IN_MILLIS) {
            mode = TimelineResult.MODE_ONE_DAY
            step = DateUtils.HOUR_IN_MILLIS
            endCalculator = { TimeUtils.getHourEnd(it) }
        } else {
            mode = TimelineResult.MODE_DAYS
            step = DateUtils.DAY_IN_MILLIS
            endCalculator = { TimeUtils.getDayEnd(it) }
        }
        for (dayTime in globalStartTime..globalEndTime step step) {
            val dayEnd = endCalculator.invoke(dayTime)
            val stamps = repository.getRaw(
                timerIds = params.timerIds,
                startTime = dayTime,
                endTime = dayEnd
            )
            val time =
                stamps.fold(0L) { acc, timerStampEntity -> acc + timerStampEntity.duration }
            result += TimelineEvent(
                timePoint = dayTime,
                duration = time,
                count = stamps.size
            )
        }

        return TimelineResult(mode = mode, events = result)
    }

    fun calculateCalendarEvents(
        params: Params
    ): LiveData<Signal<ArrayMap<Long, Int>>> = liveData {
        emit(Signal.Processing)
        emit(
            withContext(dispatcher) {
                Signal.Result(produceCalendarEvents(params))
            }
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun produceCalendarEvents(params: Params): ArrayMap<Long, Int> {
        val startTime = TimeUtils.getDayStart(params.startTime)
        val endTime = TimeUtils.getDayEnd(params.endTime)

        val result = arrayMapOf<Long, Int>()

        val records = repository.getRaw(
            timerIds = params.timerIds,
            startTime = startTime,
            endTime = endTime
        )
        records.forEach { record ->
            val recordStartTime = TimeUtils.getDayStart(record.start)
            result[recordStartTime] = result.getOrDefault(recordStartTime, 0) + 1
        }

        return result
    }

    data class DayParams(
        val timerIds: List<Int>,
        val timePoint: Long
    )

    fun calculateDayEvents(
        params: DayParams
    ): LiveData<Signal<List<TimerStampEntity>>> = liveData {
        emit(Signal.Processing)
        emit(
            withContext(dispatcher) {
                Signal.Result(produceCalendarDayEvents(params))
            }
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal suspend fun produceCalendarDayEvents(params: DayParams): List<TimerStampEntity> {
        val timePoint = params.timePoint
        val startTime = TimeUtils.getDayStart(timePoint)
        val endTime = TimeUtils.getDayEnd(timePoint)

        return repository.getRaw(
            timerIds = params.timerIds,
            startTime = startTime,
            endTime = endTime
        )
    }
}
