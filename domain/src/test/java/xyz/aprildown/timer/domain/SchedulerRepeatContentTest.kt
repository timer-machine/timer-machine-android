package xyz.aprildown.timer.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import java.security.SecureRandom

class SchedulerRepeatContentTest {

    @Test
    fun test() {
        val random = SecureRandom()
        List(50) { random.nextInt(128) }.forEach {
            val days = SchedulerEntity.everyDayToDays(it)
            val value = SchedulerEntity.daysToEveryDay(days)
            assertEquals(it, value)
        }
    }
}
