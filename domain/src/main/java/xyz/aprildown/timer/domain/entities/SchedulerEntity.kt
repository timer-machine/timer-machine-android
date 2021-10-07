package xyz.aprildown.timer.domain.entities

import java.util.Calendar

enum class SchedulerRepeatMode {
    ONCE, EVERY_WEEK, EVERY_DAYS
}

data class SchedulerEntity(
    val id: Int,
    val timerId: Int,
    val label: String,
    // 0 for start, 1 for end
    val action: Int,
    val hour: Int,
    val minute: Int,
    val repeatMode: SchedulerRepeatMode,
    val days: List<Boolean>,
    val enable: Int
) {
    companion object {
        const val ACTION_START = 0
        const val ACTION_END = 1

        const val NEW_ID = 0
        const val NULL_ID = 0

        // Little endian
        fun daysToEveryDay(days: List<Boolean>): Int {
            var result = 0
            for (index in 6 downTo 0) {
                if (days[index]) {
                    result += 1 shl index
                }
            }
            return result
        }

        fun everyDayToDays(days: Int): List<Boolean> {
            require(days in 0..127) { "$days is not between 0 and 127" }
            val bits = MutableList(7) { false }
            for (i in 6 downTo 0) {
                bits[i] = (days and (1 shl i)) != 0
            }
            return bits
        }
    }

    val isNull get() = id == NULL_ID

    fun getNextFireTime(): Calendar {
        val current = Calendar.getInstance()
        val nextTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, current.get(Calendar.YEAR))
            set(Calendar.MONTH, current.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If we are still behind the passed in currentTime, then add a day
        if (nextTime.timeInMillis <= current.timeInMillis) {
            nextTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        fun Calendar.getDistanceToDay(): Int {
            var calendarDay = get(Calendar.DAY_OF_WEEK)
            for (count in 0 until 7) {
                val index = if (calendarDay <= 1) calendarDay + 7 else calendarDay
                // Monday is 2, index is 0, so we minus 2
                if (days[index - 2]) return count
                ++calendarDay
                if (calendarDay > Calendar.SATURDAY) {
                    calendarDay = Calendar.SUNDAY
                }
            }
            return -1
        }

        // Compatible handles
        if (repeatMode == SchedulerRepeatMode.EVERY_WEEK) {
            // The day of the week might be invalid, so find next valid one
            val addDays = nextTime.getDistanceToDay()
            if (addDays > 0) {
                nextTime.add(Calendar.DAY_OF_WEEK, addDays)
            }
        } else if (repeatMode == SchedulerRepeatMode.EVERY_DAYS) {
            val addDays = daysToEveryDay(days) - 1
            if (addDays > 0) {
                nextTime.add(Calendar.DAY_OF_YEAR, addDays)
            }
        }

        // Daylight Savings Time can alter the hours and minutes when adjusting the day above.
        // Reset the desired hour and minute now that the correct day has been chosen.
        nextTime.set(Calendar.HOUR_OF_DAY, hour)
        nextTime.set(Calendar.MINUTE, minute)

        return nextTime
    }
}
