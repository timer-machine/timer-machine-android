package xyz.aprildown.timer.domain.usecases.record

import android.text.format.DateUtils
import androidx.collection.arrayMapOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.TimeUtils
import xyz.aprildown.timer.domain.TimeUtils.toLocalDateTime
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.entities.toTimerInfo
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import xyz.aprildown.timer.domain.usecases.invoke
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class RecordUseCasesTest {

    private val timerStampRepository: TimerStampRepository = mock()

    @Test(expected = UnsupportedOperationException::class)
    fun `do not call invoke`() = runTest {
        GetRecords(StandardTestDispatcher(testScheduler), timerStampRepository).invoke()
    }

    @Test
    fun `overview test`() = runTest {
        val getRecords = GetRecords(StandardTestDispatcher(testScheduler), timerStampRepository)
        val records =
            arrayMapOf<Int, List<TimerStampEntity>>()
        whenever(timerStampRepository.getRecordsGroupedByTimer(emptyList(), 0L, 0L))
            .thenReturn(records)
        getRecords.produceOverviewResult(
            GetRecords.Params(
                timerIds = emptyList(),
                startTime = 0L,
                endTime = 0L
            )
        )

        // We need some tests here, but they're so complicated to write.

        verify(timerStampRepository).getRecordsGroupedByTimer(emptyList(), 0L, 0L)

        verifyNoMoreInteractions(timerStampRepository)
    }

    @Test
    fun `timeline days test`() = runTest(timeout = 30.seconds) {
        val getRecords = GetRecords(StandardTestDispatcher(testScheduler), timerStampRepository)

        val start = LocalDateTime.of(
            YearMonth.of(2018, Month.NOVEMBER).atDay(1),
            LocalTime.MIN
        ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val predict =
            arrayMapOf<Long, MutableList<TimerStampEntity>>()
        repeat(500) {
            val time = Random.nextLong(now - start) + start
            val duration = Random.nextLong(DateUtils.DAY_IN_MILLIS * 2)
            val dayStart = TimeUtils.getDayStart(time)
            predict[dayStart] = predict.getOrDefault(dayStart, mutableListOf()).apply {
                add(
                    TimerStampEntity(
                        id = TestData.fakeTimerStampIdA,
                        timerId = TestData.fakeTimerId,
                        start = time,
                        end = time + duration
                    )
                )
            }
        }

        for (dayTime in TimeUtils.getDayStart(start)..TimeUtils.getDayEnd(now) step DateUtils.DAY_IN_MILLIS) {
            whenever(
                timerStampRepository.getRaw(
                    any(),
                    eq(dayTime),
                    eq(TimeUtils.getDayEnd(dayTime))
                )
            ).thenReturn(predict[dayTime] ?: emptyList())
        }

        val result = getRecords.produceTimelineResult(
            GetRecords.Params(
                timerIds = emptyList(),
                startTime = start,
                endTime = now
            )
        )
        assertEquals(GetRecords.TimelineResult.MODE_DAYS, result.mode)
        val resultEvents =
            result.events.filter { it.duration > 0 && it.count > 0 }
        assertEquals(predict.keys.size, resultEvents.size)
        predict.keys.forEach { predictTime ->
            val stamps = predict[predictTime]!!
            val event = resultEvents.find { it.timePoint == predictTime }
            assertEquals(
                stamps.fold(0L) { acc, timerStampEntity -> acc + timerStampEntity.duration },
                event?.duration
            )
            assertEquals(stamps.size, event?.count)
        }

        for (dayTime in TimeUtils.getDayStart(start)..TimeUtils.getDayEnd(now) step DateUtils.DAY_IN_MILLIS) {
            verify(timerStampRepository).getRaw(
                any(),
                eq(dayTime),
                eq(TimeUtils.getDayEnd(dayTime))
            )
        }

        verifyNoMoreInteractions(timerStampRepository)
    }

    @Test
    fun `timeline one day test`() = runTest {
        val getRecords = GetRecords(StandardTestDispatcher(testScheduler), timerStampRepository)

        val now = System.currentTimeMillis()
        val dayStart = TimeUtils.getDayStart(now)

        val predict =
            arrayMapOf<Long, MutableList<TimerStampEntity>>()
        repeat(500) {
            val time = Random.nextLong(DateUtils.DAY_IN_MILLIS) + dayStart
            val duration = Random.nextLong(DateUtils.DAY_IN_MILLIS * 2)
            val hourStart = TimeUtils.getHourStart(time)
            predict[hourStart] = predict.getOrDefault(hourStart, mutableListOf()).apply {
                add(
                    TimerStampEntity(
                        id = TestData.fakeTimerStampIdA,
                        timerId = TestData.fakeTimerId,
                        start = time,
                        end = time + duration
                    )
                )
            }
        }

        predict.keys.forEach { hourStart ->
            whenever(
                timerStampRepository.getRaw(
                    any(),
                    eq(hourStart),
                    eq(TimeUtils.getHourEnd(hourStart))
                )
            ).thenReturn(predict[hourStart])
        }
        val result = getRecords.produceTimelineResult(
            GetRecords.Params(
                timerIds = emptyList(),
                startTime = now,
                endTime = now
            )
        )
        assertEquals(GetRecords.TimelineResult.MODE_ONE_DAY, result.mode)
        val resultEvents = result.events
        assertEquals(predict.keys.size, resultEvents.size)
        predict.keys.forEach { predictTime ->
            val stamps = predict[predictTime]!!
            val event = resultEvents.find { it.timePoint == predictTime }
            assertEquals(
                stamps.fold(0L) { acc, timerStampEntity -> acc + timerStampEntity.duration },
                event?.duration
            )
            assertEquals(stamps.size, event?.count)
        }

        predict.keys.forEach { hourStart ->
            verify(timerStampRepository).getRaw(
                any(),
                eq(hourStart),
                eq(TimeUtils.getHourEnd(hourStart))
            )
        }

        verifyNoMoreInteractions(timerStampRepository)
    }

    @Test
    fun `calendar month test`() = runTest {
        val getRecords = GetRecords(StandardTestDispatcher(testScheduler), timerStampRepository)

        val now = System.currentTimeMillis()
        val monthStart = TimeUtils.getMonthStart(now)
        val monthEnd = TimeUtils.getMonthEnd(now)

        val predict = arrayMapOf<Long, Int>()
        val timerStamps = List(Random.nextInt(500)) {
            val time = Random.nextLong(monthEnd - monthStart + 1) + monthStart
            val dayStart = TimeUtils.getDayStart(time)
            predict[dayStart] = predict.getOrDefault(dayStart, 0) + 1
            TimerStampEntity(
                id = TestData.fakeTimerStampIdA,
                timerId = TestData.fakeTimerId,
                start = time,
                end = time
            )
        }

        whenever(timerStampRepository.getRaw(emptyList(), monthStart, monthEnd))
            .thenReturn(timerStamps)
        val result = getRecords.produceCalendarEvents(
            GetRecords.Params(
                timerIds = emptyList(),
                startTime = monthStart,
                endTime = monthEnd
            )
        )

        assertTrue(
            result.keys.size <=
                now.toLocalDateTime().let { YearMonth.of(it.year, it.monthValue).lengthOfMonth() }
        )
        result.keys.forEach {
            val dateTime =
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
            assertEquals(0, dateTime.hour)
            assertEquals(0, dateTime.minute)
            assertEquals(0, dateTime.second)
        }
        result.values.forEach {
            assertTrue(it > 0)
        }
        assertEquals(predict.keys.size, result.keys.size)
        predict.keys.forEach {
            assertEquals(predict[it], result[it])
        }

        verify(timerStampRepository).getRaw(emptyList(), monthStart, monthEnd)

        verifyNoMoreInteractions(timerStampRepository)
    }

    @Test
    fun `calendar day test`() = runTest {
        val getRecords = GetRecords(StandardTestDispatcher(testScheduler), timerStampRepository)
        val now = System.currentTimeMillis()
        val timerStamps =
            List(Random.nextInt(50)) { TestData.getRandomDaysTimerStamp(from = now) }
        val dayStart = TimeUtils.getDayStart(now)
        val dayEnd = TimeUtils.getDayEnd(now)
        whenever(timerStampRepository.getRaw(emptyList(), dayStart, dayEnd))
            .thenReturn(timerStamps)
        val result = getRecords.produceCalendarDayEvents(
            GetRecords.DayParams(
                timerIds = emptyList(),
                timePoint = now
            )
        )
        assertEquals(timerStamps, result)

        verify(timerStampRepository).getRaw(emptyList(), dayStart, dayEnd)

        verifyNoMoreInteractions(timerStampRepository)
    }

    @Test
    fun add() = runTest {
        val timerRepo: TimerRepository = mock()
        val appDataRepository: AppDataRepository = mock()
        val stamp = TestData.fakeTimerStampA
        whenever(timerRepo.getTimerInfoByTimerId(stamp.timerId))
            .thenReturn(TestData.fakeTimerSimpleA.toTimerInfo())
        whenever(timerStampRepository.add(stamp)).thenReturn(1)
        assertEquals(
            1,
            AddTimerStamp(
                StandardTestDispatcher(testScheduler),
                timerStampRepository,
                timerRepo,
                appDataRepository
            ).execute(stamp)
        )
        verify(timerRepo).getTimerInfoByTimerId(stamp.timerId)

        verify(timerStampRepository).add(stamp)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(timerRepo)
        verifyNoMoreInteractions(timerStampRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun `add to no timer`() = runTest {
        val timerRepo: TimerRepository = mock()
        val appDataRepository: AppDataRepository = mock()

        val timerStamp = TestData.fakeTimerStampA

        whenever(timerRepo.getTimerInfoByTimerId(timerStamp.timerId)).thenReturn(null)
        val useCase =
            AddTimerStamp(
                StandardTestDispatcher(testScheduler),
                timerStampRepository,
                timerRepo,
                appDataRepository
            )

        useCase.execute(timerStamp)

        verify(timerRepo).getTimerInfoByTimerId(timerStamp.timerId)

        verifyNoMoreInteractions(timerRepo)
        verifyNoMoreInteractions(timerStampRepository)
        verifyNoMoreInteractions(appDataRepository)
    }
}
