package xyz.aprildown.timer.app.base.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.DateFormatSymbols
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY
import java.util.Collections
import kotlin.random.Random

class WeekdaysFormatterTest {

    private lateinit var formatter: WeekdaysFormatter

    private val everyDatString = "Every Day"

    @Test
    fun produceDataStringsMondayTest() {
        formatter = WeekdaysFormatter(MONDAY, everyDatString)
        assertEquals(
            "",
            formatter.produceDataStrings(listOf(false, false, false, false, false, false, false))
        )
        assertEquals(
            everyDatString,
            formatter.produceDataStrings(listOf(true, true, true, true, true, true, true))
        )
        assertEquals(joinWeekdays(1, 2, 3, 4, 5), formatter.produceDataStrings(days1))
        assertEquals(joinWeekdays(1, 3, 5, 7), formatter.produceDataStrings(days2))
        assertEquals(joinWeekdays(3, 4, 6), formatter.produceDataStrings(days3))
        assertEquals(joinWeekdays(1, 3, 4, 6), formatter.produceDataStrings(days4))
        assertEquals(joinWeekdays(2, 4, 6), formatter.produceDataStrings(days5))
        assertEquals(joinWeekdays(1, 2, 4, 6, 7), formatter.produceDataStrings(days6))
    }

    @Test
    fun produceDataStringsTuesdayTest() {
        formatter = WeekdaysFormatter(TUESDAY, everyDatString)
        assertEquals(joinWeekdays(2, 3, 4, 5, 1), formatter.produceDataStrings(days1))
        assertEquals(joinWeekdays(3, 5, 7, 1), formatter.produceDataStrings(days2))
        assertEquals(joinWeekdays(3, 4, 6), formatter.produceDataStrings(days3))
        assertEquals(joinWeekdays(3, 4, 6, 1), formatter.produceDataStrings(days4))
        assertEquals(joinWeekdays(2, 4, 6), formatter.produceDataStrings(days5))
        assertEquals(joinWeekdays(2, 4, 6, 7, 1), formatter.produceDataStrings(days6))
    }

    @Test
    fun produceDataStringsWednesdayTest() {
        formatter = WeekdaysFormatter(WEDNESDAY, everyDatString)
        assertEquals(joinWeekdays(3, 4, 5, 1, 2), formatter.produceDataStrings(days1))
        assertEquals(joinWeekdays(3, 5, 7, 1), formatter.produceDataStrings(days2))
        assertEquals(joinWeekdays(3, 4, 6), formatter.produceDataStrings(days3))
        assertEquals(joinWeekdays(3, 4, 6, 1), formatter.produceDataStrings(days4))
        assertEquals(joinWeekdays(4, 6, 2), formatter.produceDataStrings(days5))
        assertEquals(joinWeekdays(4, 6, 7, 1, 2), formatter.produceDataStrings(days6))
    }

    @Test
    fun produceDataStringsThursdayTest() {
        formatter = WeekdaysFormatter(THURSDAY, everyDatString)
        assertEquals(joinWeekdays(4, 5, 1, 2, 3), formatter.produceDataStrings(days1))
        assertEquals(joinWeekdays(5, 7, 1, 3), formatter.produceDataStrings(days2))
        assertEquals(joinWeekdays(4, 6, 3), formatter.produceDataStrings(days3))
        assertEquals(joinWeekdays(4, 6, 1, 3), formatter.produceDataStrings(days4))
        assertEquals(joinWeekdays(4, 6, 2), formatter.produceDataStrings(days5))
        assertEquals(joinWeekdays(4, 6, 7, 1, 2), formatter.produceDataStrings(days6))
    }

    @Test
    fun produceDataStringsFridayTest() {
        formatter = WeekdaysFormatter(FRIDAY, everyDatString)
        assertEquals(joinWeekdays(5, 1, 2, 3, 4), formatter.produceDataStrings(days1))
        assertEquals(joinWeekdays(5, 7, 1, 3), formatter.produceDataStrings(days2))
        assertEquals(joinWeekdays(6, 3, 4), formatter.produceDataStrings(days3))
        assertEquals(joinWeekdays(6, 1, 3, 4), formatter.produceDataStrings(days4))
        assertEquals(joinWeekdays(6, 2, 4), formatter.produceDataStrings(days5))
        assertEquals(joinWeekdays(6, 7, 1, 2, 4), formatter.produceDataStrings(days6))
    }

    @Test
    fun produceDataStringsSaturdayTest() {
        formatter = WeekdaysFormatter(SATURDAY, everyDatString)
        assertEquals(joinWeekdays(1, 2, 3, 4, 5), formatter.produceDataStrings(days1))
        assertEquals(joinWeekdays(7, 1, 3, 5), formatter.produceDataStrings(days2))
        assertEquals(joinWeekdays(6, 3, 4), formatter.produceDataStrings(days3))
        assertEquals(joinWeekdays(6, 1, 3, 4), formatter.produceDataStrings(days4))
        assertEquals(joinWeekdays(6, 2, 4), formatter.produceDataStrings(days5))
        assertEquals(joinWeekdays(6, 7, 1, 2, 4), formatter.produceDataStrings(days6))
    }

    @Test
    fun produceDataStringsSundayTest() {
        formatter = WeekdaysFormatter(SUNDAY, everyDatString)
        assertEquals(joinWeekdays(1, 2, 3, 4, 5), formatter.produceDataStrings(days1))
        assertEquals(joinWeekdays(7, 1, 3, 5), formatter.produceDataStrings(days2))
        assertEquals(joinWeekdays(3, 4, 6), formatter.produceDataStrings(days3))
        assertEquals(joinWeekdays(1, 3, 4, 6), formatter.produceDataStrings(days4))
        assertEquals(joinWeekdays(2, 4, 6), formatter.produceDataStrings(days5))
        assertEquals(joinWeekdays(7, 1, 2, 4, 6), formatter.produceDataStrings(days6))
    }

    @Test
    fun insaneTest() {
        randomTest(MONDAY)
        randomTest(TUESDAY)
        randomTest(WEDNESDAY)
        randomTest(THURSDAY)
        randomTest(FRIDAY)
        randomTest(SATURDAY)
        randomTest(SUNDAY)
    }

    private fun randomTest(startWeekOn: Int) {
        val formatter = WeekdaysFormatter(startWeekOn, everyDatString)
        val weekDayOrderDays = WeekdaysFormatter.WeekDayOrder.Monday().days

        repeat(50) {
            val days = MutableList(7) { Random.nextBoolean() }

            val resultList = mutableListOf<String?>()
            days.forEachIndexed { index, b ->
                if (b) {
                    resultList += daysNames[weekDayOrderDays[index]]
                } else {
                    resultList.add(null)
                }
            }
            Collections.rotate(
                resultList,
                if (startWeekOn == SUNDAY) -6 else -(startWeekOn - 2)
            )
            val resultString = resultList.filterNotNull().joinToString(", ")

            assertEquals(
                when {
                    resultList.all { it == null } -> ""
                    resultList.all { it != null } -> everyDatString
                    else -> resultString
                },
                formatter.produceDataStrings(days)
            )
        }
    }

    private fun joinWeekdays(vararg days: Int): String {
        val strings = mutableListOf<String>()
        for (day in days) {
            val index = when (day) {
                1, 2, 3, 4, 5, 6 -> day + 1
                7 -> 1
                else -> throw IllegalArgumentException("Wrong day: $day")
            }
            strings.add(daysNames[index])
        }
        return strings.joinToString(separator = ", ")
    }
}

private val daysNames = DateFormatSymbols().shortWeekdays
private val days1 = listOf(true, true, true, true, true, false, false)
private val days2 = listOf(true, false, true, false, true, false, true)
private val days3 = listOf(false, false, true, true, false, true, false)
private val days4 = listOf(true, false, true, true, false, true, false)
private val days5 = listOf(false, true, false, true, false, true, false)
private val days6 = listOf(true, true, false, true, false, true, true)
