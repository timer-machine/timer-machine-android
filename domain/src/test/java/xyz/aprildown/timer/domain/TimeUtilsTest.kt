package xyz.aprildown.timer.domain

import android.text.format.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.aprildown.timer.domain.TimeUtils.toEpochMilli
import xyz.aprildown.timer.domain.TimeUtils.toLocalDateTime
import java.time.YearMonth

class TimeUtilsTest {

    @Test
    fun `Long and LocalDateTime`() {
        val now = System.currentTimeMillis()
        val localDateTime = now.toLocalDateTime()
        assertEquals(localDateTime.toEpochMilli(), now)
        assertEquals(localDateTime.toEpochMilli().toLocalDateTime(), localDateTime)
    }

    @Test
    fun `Hour start and end`() {
        val now = System.currentTimeMillis()
        val hourStart = TimeUtils.getHourStart(now)
        val hourEnd = TimeUtils.getHourEnd(now)
        assertEquals(hourEnd - hourStart + 1, DateUtils.HOUR_IN_MILLIS)
    }

    @Test
    fun `Day start and end`() {
        val now = System.currentTimeMillis()
        val dayStart = TimeUtils.getDayStart(now)
        val dayEnd = TimeUtils.getDayEnd(now)
        assertEquals(dayEnd - dayStart + 1, DateUtils.DAY_IN_MILLIS)
    }

    @Test
    fun `Month start and end`() {
        val now = System.currentTimeMillis()
        val mothStart = TimeUtils.getMonthStart(now)
        val mothEnd = TimeUtils.getMonthEnd(now)

        assertEquals(
            mothEnd - mothStart + 1,
            YearMonth.now().lengthOfMonth() * DateUtils.DAY_IN_MILLIS
        )
    }
}
