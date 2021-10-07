package xyz.aprildown.timer.domain

import android.content.Context
import android.text.format.DateUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

// region java.time

object TimeUtils {

    fun Long.toLocalDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
    }

    fun LocalDateTime.toEpochMilli(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun getHourStart(time: Long): Long {
        val currentDateTime = time.toLocalDateTime()
        return LocalDateTime.of(
            currentDateTime.toLocalDate(),
            LocalTime.of(currentDateTime.toLocalTime().hour, 0)
        ).toEpochMilli()
    }

    fun getHourEnd(time: Long): Long {
        val oneHourLaterDateTime = time.toLocalDateTime().plusHours(1)
        return LocalDateTime.of(
            oneHourLaterDateTime.toLocalDate(),
            LocalTime.of(oneHourLaterDateTime.toLocalTime().hour, 0)
        ).toEpochMilli() - 1
    }

    fun getDayStart(time: Long): Long {
        return time.toLocalDateTime().toLocalDate().atStartOfDay().toEpochMilli()
    }

    fun getDayEnd(time: Long): Long {
        return time.toLocalDateTime().toLocalDate()
            .plusDays(1).atStartOfDay().toEpochMilli() - 1
    }

    fun getMonthStart(time: Long): Long {
        val localDateTime = time.toLocalDateTime()
        return YearMonth.of(localDateTime.year, localDateTime.month)
            .atDay(1).atStartOfDay().toEpochMilli()
    }

    fun getMonthEnd(time: Long): Long {
        val localDateTime = time.toLocalDateTime()
        return YearMonth.of(localDateTime.year, localDateTime.month)
            .plusMonths(1)
            .atDay(1).atStartOfDay().toEpochMilli() - 1
    }

    fun formattedTodayTime(context: Context, hour: Int, minute: Int): String {
        return DateUtils.formatDateTime(
            context,
            getDayStart(System.currentTimeMillis()) +
                hour * DateUtils.HOUR_IN_MILLIS +
                minute * DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_SHOW_TIME
        )
    }
}

// endregion java.time
