package xyz.aprildown.timer.app.base.utils

import android.content.Context
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.timer.app.base.data.PreferenceData.startWeekOn
import java.text.DateFormatSymbols
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY

/**
 * Weekdays data in db is from Monday(2)
 * Missions: week start setting <=> Monday, string format
 */
class WeekdaysFormatter(
    private val startWeekOn: Int,
    private val everyDayString: String
) {

    /**
     * Produce repeat string
     * Only including string being needed to be joined
     * @data db format
     */
    fun produceDataStrings(data: List<Boolean>): String {
        // Not repeating
        if (!data.any { it }) return ""
        // Every day
        if (data.all { it }) return everyDayString

        val longNames = data.size <= 1
        val dfs = DateFormatSymbols()

        // Sunday is index 1
        val weekdays = if (longNames) dfs.weekdays else dfs.shortWeekdays
        val weekdayOrder = WeekDayOrder.fromStartDay(startWeekOn)

        val builder = StringBuilder(40)
        for (calendarDay in weekdayOrder.days) {
            if (data[calendarDayToDayIndex(calendarDay)]) {
                if (builder.isNotEmpty()) {
                    builder.append(", ")
                }
                builder.append(weekdays[calendarDay])
            }
        }
        return builder.toString()
    }

    sealed class WeekDayOrder(val days: IntArray) {
        class Monday : WeekDayOrder(
            intArrayOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
        )

        class Tuesday : WeekDayOrder(
            intArrayOf(TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, MONDAY)
        )

        class Wednesday : WeekDayOrder(
            intArrayOf(WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, MONDAY, TUESDAY)
        )

        class Thursday : WeekDayOrder(
            intArrayOf(THURSDAY, FRIDAY, SATURDAY, SUNDAY, MONDAY, TUESDAY, WEDNESDAY)
        )

        class Friday : WeekDayOrder(
            intArrayOf(FRIDAY, SATURDAY, SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY)
        )

        class Saturday : WeekDayOrder(
            intArrayOf(SATURDAY, SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
        )

        class Sunday : WeekDayOrder(
            intArrayOf(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)
        )

        companion object {
            fun fromStartDay(calendarDay: Int): WeekDayOrder = when (calendarDay) {
                MONDAY -> Monday()
                TUESDAY -> Tuesday()
                WEDNESDAY -> Wednesday()
                THURSDAY -> Thursday()
                FRIDAY -> Friday()
                SATURDAY -> Saturday()
                SUNDAY -> Sunday()
                else -> throw IllegalArgumentException("Unknown calendar day $calendarDay")
            }
        }
    }

    companion object {

        /**
         * [MONDAY] 2 [TUESDAY] 3 [WEDNESDAY] 4 [THURSDAY] 5 [FRIDAY] 6 [SATURDAY] 7 [SUNDAY] 1
         */
        fun calendarDayToDayIndex(calendarDay: Int): Int {
            require(calendarDay in 1..7)
            return if (calendarDay == 1) 6 else calendarDay - 2
        }

        fun createFromContext(context: Context): WeekdaysFormatter {
            return WeekdaysFormatter(
                startWeekOn = context.startWeekOn,
                everyDayString = context.getString(R.string.scheduler_repeat_every_day)
            )
        }
    }
}
