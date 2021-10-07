package xyz.aprildown.timer.domain.entities

import xyz.aprildown.timer.domain.TimeUtils.toEpochMilli
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth

data class TimerStampEntity(
    val id: Int,
    val timerId: Int,
    val start: Long,
    val end: Long
) {

    val duration: Long get() = if (start == 0L) 0 else end - start

    constructor(timerId: Int, startTime: Long) : this(
        NEW_ID,
        timerId,
        startTime,
        System.currentTimeMillis()
    )

    companion object {
        const val NEW_ID = 0
        const val NULL_ID = 0

        fun getMinDateMilli(): Long {
            return LocalDateTime.of(
                YearMonth.of(2018, Month.NOVEMBER).atDay(1),
                LocalTime.MIN
            ).toEpochMilli()
        }
    }
}
