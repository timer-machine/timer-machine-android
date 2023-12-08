package xyz.aprildown.timer.domain.entities

/**
 * MUSIC:
 *     str1: music name
 *     str2: music uri(content://abc...)
 *     bool: loop or not
 * VIBRATION:
 *     star1: vibrate n times
 *     str2: vibration repeat pattern
 * SCREEN:
 *     str1: fullscreen or not (we use str1 instead of bool because bool is true by default)
 * VOICE:
 *     str1: speak content
 * BEEP:
 *     str1: ""
 *     str2: 1,5(beep count, 0 or "" for beep all, beep sound index)
 *     bool: pause other background sound or not
 * HALF:
 *     str1: half option
 * COUNT:
 *     str1: count the last n seconds
 * NOTIFICATION:
 *     str1: last n seconds, 0 for always
 * FLASHLIGHT:
 *     empty for now
 */
enum class BehaviourType {
    MUSIC, VIBRATION, SCREEN, HALT, VOICE, BEEP, HALF, COUNT, NOTIFICATION, FLASHLIGHT;

    val hasBoolValue: Boolean
        get() = this == MUSIC || this == BEEP

    val defaultBoolValue: Boolean
        get() = when (this) {
            MUSIC, BEEP -> true
            else -> throw IllegalArgumentException("Check hasBoolValue before defaultBoolValue")
        }
}

data class BehaviourEntity(
    val type: BehaviourType,
    val str1: String = "",
    val str2: String = "",
    val bool: Boolean = true
)

private interface Action {
    fun toBehaviourEntity(): BehaviourEntity
}

// region Music

data class MusicAction(val title: String = "", val uri: String = "", val loop: Boolean = true) :
    Action {
    override fun toBehaviourEntity(): BehaviourEntity {
        return BehaviourEntity(BehaviourType.MUSIC, title, uri, loop)
    }
}

fun BehaviourEntity.toMusicAction(): MusicAction {
    require(type == BehaviourType.MUSIC)
    return MusicAction(str1, str2, bool)
}

// endregion Music

// region Vibration

private const val VIBRATION_SHORT = 100L
private const val VIBRATION_NORMAL = 250L
private const val VIBRATION_LONG = 500L

data class VibrationAction(
    val count: Int = 0,
    val vibrationPattern: VibrationPattern = VibrationPattern.Normal()
) : Action {

    sealed class VibrationPattern(val pattern: LongArray) {
        class Short : VibrationPattern(longArrayOf(VIBRATION_SHORT, VIBRATION_SHORT))
        class Normal : VibrationPattern(longArrayOf(VIBRATION_NORMAL, VIBRATION_NORMAL))
        class Long : VibrationPattern(longArrayOf(VIBRATION_LONG, VIBRATION_LONG))

        val twicePattern: LongArray get() = pattern + pattern
    }

    fun calculateVibratorPattern(): LongArray {
        val basePattern = vibrationPattern.pattern
        return if (count == 0) {
            basePattern
        } else {
            val size = basePattern.size
            LongArray(1 + size * count) { index ->
                if (index == 0) 0L else basePattern[(index - 1) % size]
            }
        }
    }

    override fun toBehaviourEntity(): BehaviourEntity {
        return BehaviourEntity(
            BehaviourType.VIBRATION,
            str1 = if (count == 0) "" else count.toString(),
            str2 = when (vibrationPattern) {
                is VibrationPattern.Normal -> ""
                else -> vibrationPattern.pattern.joinToString(separator = " ")
            }
        )
    }
}

fun BehaviourEntity.toVibrationAction(): VibrationAction {
    require(type == BehaviourType.VIBRATION)

    return VibrationAction(
        count = str1.toIntOrNull() ?: 0,
        vibrationPattern = if (str2.isBlank()) {
            VibrationAction.VibrationPattern.Normal()
        } else {
            val array = str2.split(" ").map { it.toLong() }.toLongArray()

            val short = VibrationAction.VibrationPattern.Short()
            if (array.contentEquals(short.pattern)) {
                short
            } else {
                val long = VibrationAction.VibrationPattern.Long()
                if (array.contentEquals(long.pattern)) {
                    long
                } else {
                    VibrationAction.VibrationPattern.Normal()
                }
            }
        }
    )
}

// endregion Vibration

// region Screen

data class ScreenAction(val fullScreen: Boolean = false) : Action {
    override fun toBehaviourEntity(): BehaviourEntity {
        return BehaviourEntity(BehaviourType.SCREEN, str1 = if (fullScreen) "1" else "0")
    }
}

fun BehaviourEntity.toScreenAction(): ScreenAction {
    require(type == BehaviourType.SCREEN)
    return ScreenAction(str1.toIntOrNull() == 1)
}

// endregion Screen

// region Voice

data class VoiceAction(
    val content: String = "",
    val content2: String = "",
) : Action {
    override fun toBehaviourEntity(): BehaviourEntity {
        return BehaviourEntity(BehaviourType.VOICE, str1 = content, str2 = content2)
    }

    companion object {

        // region Replacer deprecated

        const val REPLACER_LOOP = "\$loop"
        const val REPLACER_TOTAL_LOOP = "\$total_loop"
        const val REPLACER_STEP_NAME = "\$step_name"
        const val REPLACER_STEP_DURATION = "\$step_duration"

        const val REPLACER_ELAPSED_TIME = "\$elapsed"
        const val REPLACER_ELAPSED_TIME_GROUP = "\$elapsed_group"
        const val REPLACER_ELAPSED_TIME_PERCENT_OLD = "\$elapsed_in_percent"
        const val REPLACER_ELAPSED_TIME_PERCENT = "\$elapsed%"
        const val REPLACER_ELAPSED_TIME_PERCENT_GROUP = "\$elapsed%_group"

        const val REPLACER_REMAINING_TIME = "\$remaining"
        const val REPLACER_REMAINING_TIME_GROUP = "\$remaining_group"
        const val REPLACER_REMAINING_TIME_PERCENT_OLD = "\$remaining_in_percent"
        const val REPLACER_REMAINING_TIME_PERCENT = "\$remaining%"
        const val REPLACER_REMAINING_TIME_PERCENT_GROUP = "\$remaining%_group"

        const val REPLACER_CURRENT_TIME = "\$time"
        const val REPLACER_STEP_END_TIME = "\$step_end_time"
        const val REPLACER_TIMER_END_TIME = "\$timer_end_time"
        const val REPLACER_GROUP_END_TIME = "\$group_end_time"

        // endregion Replacer deprecated

        // region Variable

        const val VARIABLE_STEP_NAME = "{step_name}"
        const val VARIABLE_STEP_DURATION = "{step_duration}"
        const val VARIABLE_STEP_END_TIME = "{step_end_time}"

        const val VARIABLE_TIMER_NAME = "{timer_name}"
        const val VARIABLE_TIMER_LOOP = "{timer_loop}"
        const val VARIABLE_TIMER_TOTAL_LOOP = "{timer_loop_total}"
        const val VARIABLE_TIMER_DURATION = "{timer_duration}"
        const val VARIABLE_TIMER_ELAPSED = "{timer_elapsed}"
        const val VARIABLE_TIMER_ELAPSED_PERCENT = "{timer_elapsed%}"
        const val VARIABLE_TIMER_REMAINING = "{timer_remaining}"
        const val VARIABLE_TIMER_REMAINING_PERCENT = "{timer_remaining%}"
        const val VARIABLE_TIMER_END_TIME = "{timer_end_time}"

        const val VARIABLE_GROUP_NAME = "{group_name}"
        const val VARIABLE_GROUP_LOOP = "{group_loop}"
        const val VARIABLE_GROUP_TOTAL_LOOP = "{group_loop_total}"
        const val VARIABLE_GROUP_DURATION = "{group_duration}"
        const val VARIABLE_GROUP_ELAPSED = "{group_elapsed}"
        const val VARIABLE_GROUP_ELAPSED_PERCENT = "{group_elapsed%}"
        const val VARIABLE_GROUP_REMAINING = "{group_remaining}"
        const val VARIABLE_GROUP_REMAINING_PERCENT = "{group_remaining%}"
        const val VARIABLE_GROUP_END_TIME = "{group_end_time}"

        const val VARIABLE_CLOCK_TIME = "{clock_time}"

        // endregion Variable

        // region Voice Variable

        const val VOICE_VARIABLE_TIMER_NAME = "{TName}"
        const val VOICE_VARIABLE_TIMER_LOOP = "{TLoop}"
        const val VOICE_VARIABLE_TIMER_TOTAL_LOOP = "{TTotalLoop}"
        const val VOICE_VARIABLE_TIMER_DURATION = "{TDuration}"
        const val VOICE_VARIABLE_TIMER_ELAPSED = "{TElapsed}"
        const val VOICE_VARIABLE_TIMER_ELAPSED_PERCENT = "{TElapsed%}"
        const val VOICE_VARIABLE_TIMER_REMAINING = "{TRemaining}"
        const val VOICE_VARIABLE_TIMER_REMAINING_PERCENT = "{TRemaining%}"
        const val VOICE_VARIABLE_TIMER_END_TIME = "{TEndTime}"

        const val VOICE_VARIABLE_GROUP_NAME = "{GName}"
        const val VOICE_VARIABLE_GROUP_LOOP = "{GLoop}"
        const val VOICE_VARIABLE_GROUP_TOTAL_LOOP = "{GTotalLoop}"
        const val VOICE_VARIABLE_GROUP_DURATION = "{GDuration}"
        const val VOICE_VARIABLE_GROUP_ELAPSED = "{GElapsed}"
        const val VOICE_VARIABLE_GROUP_ELAPSED_PERCENT = "{GElapsed%}"
        const val VOICE_VARIABLE_GROUP_REMAINING = "{GRemaining}"
        const val VOICE_VARIABLE_GROUP_REMAINING_PERCENT = "{GRemaining%}"
        const val VOICE_VARIABLE_GROUP_END_TIME = "{GEndTime}"

        const val VOICE_VARIABLE_STEP_NAME = "{SName}"
        const val VOICE_VARIABLE_STEP_NAME_NEXT = "{SNameNext}"
        const val VOICE_VARIABLE_STEP_DURATION = "{SDuration}"
        const val VOICE_VARIABLE_STEP_END_TIME = "{SEndTime}"

        const val VOICE_VARIABLE_OTHER_CLOCK_TIME = "{OClockTime}"

        // endregion VoiceVariable
    }
}

fun BehaviourEntity.toVoiceAction(): VoiceAction {
    require(type == BehaviourType.VOICE)
    return VoiceAction(content = str1, content2 = str2)
}

// endregion Voice

// region Beep

data class BeepAction(
    val count: Int = 0,
    val soundIndex: Int = 0,
    val respectOtherSound: Boolean = true
) : Action {
    override fun toBehaviourEntity(): BehaviourEntity {
        return BehaviourEntity(BehaviourType.BEEP, "", "$count,$soundIndex", respectOtherSound)
    }
}

fun BehaviourEntity.toBeepAction(): BeepAction {
    require(type == BehaviourType.BEEP)
    require(str1.isEmpty())
    val parts = str2.split(",")
    return BeepAction(
        count = parts.getOrNull(0)?.toIntOrNull() ?: 0,
        soundIndex = parts.getOrNull(1)?.toIntOrNull() ?: 0,
        respectOtherSound = bool
    )
}

// endregion Beep

// region Half

data class HalfAction(val option: Int = OPTION_VOICE) : Action {
    override fun toBehaviourEntity(): BehaviourEntity {
        return BehaviourEntity(
            BehaviourType.HALF,
            str1 = if (option == OPTION_VOICE) "" else option.toString()
        )
    }

    companion object {
        const val OPTION_VOICE = 0
        const val OPTION_MUSIC = 1
        const val OPTION_VIBRATION = 2
    }
}

fun BehaviourEntity.toHalfAction(): HalfAction {
    require(type == BehaviourType.HALF)
    return HalfAction(str1.toIntOrNull() ?: HalfAction.OPTION_VOICE)
}

// endregion Half

// region Count

data class CountAction(
    val times: Int = DEFAULT_TIMES,
    val beep: Boolean = false,
) : Action {
    override fun toBehaviourEntity(): BehaviourEntity {
        return BehaviourEntity(
            BehaviourType.COUNT,
            str1 = if (times == DEFAULT_TIMES) "" else times.toString(),
            str2 = if (beep) "1" else "0",
        )
    }

    companion object {
        const val DEFAULT_TIMES = 5
    }
}

fun BehaviourEntity.toCountAction(): CountAction {
    require(type == BehaviourType.COUNT)
    return CountAction(
        times = str1.toIntOrNull() ?: CountAction.DEFAULT_TIMES,
        beep = str2.toIntOrNull() == 1,
    )
}

// endregion Count

// region Notification

/**
 * @param duration The seconds after which the notification will be dismissed.
 *                 0 for always.
 */
data class NotificationAction(val duration: Int = 0) : Action {
    override fun toBehaviourEntity(): BehaviourEntity {
        return BehaviourEntity(
            BehaviourType.NOTIFICATION,
            str1 = if (duration == 0) "" else duration.toString()
        )
    }
}

fun BehaviourEntity.toNotificationAction(): NotificationAction {
    require(type == BehaviourType.NOTIFICATION)
    return NotificationAction(str1.toIntOrNull() ?: 0)
}

// endregion Notification

// region Notification

data class FlashlightAction(val step: Long = 500L) : Action {
    override fun toBehaviourEntity(): BehaviourEntity {
        return BehaviourEntity(BehaviourType.FLASHLIGHT)
    }
}

fun BehaviourEntity.toFlashlightAction(): FlashlightAction {
    require(type == BehaviourType.FLASHLIGHT)
    return FlashlightAction()
}

// endregion Notification
