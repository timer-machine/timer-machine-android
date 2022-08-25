package xyz.aprildown.timer.component.key.behaviour

import android.content.Context
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.CountAction
import xyz.aprildown.timer.domain.entities.HalfAction
import xyz.aprildown.timer.domain.entities.VibrationAction
import xyz.aprildown.timer.domain.entities.toBeepAction
import xyz.aprildown.timer.domain.entities.toCountAction
import xyz.aprildown.timer.domain.entities.toHalfAction
import xyz.aprildown.timer.domain.entities.toMusicAction
import xyz.aprildown.timer.domain.entities.toNotificationAction
import xyz.aprildown.timer.domain.entities.toVibrationAction
import xyz.aprildown.timer.domain.entities.toVoiceAction
import xyz.aprildown.timer.app.base.R as RBase

internal fun BehaviourEntity.getChipText(context: Context): String {

    fun getDefaultName(): String = context.getString(type.nameRes)

    return when (type) {
        BehaviourType.MUSIC -> {
            toMusicAction().title.ifBlank { null }
        }
        BehaviourType.VIBRATION -> {
            buildString {
                val action = toVibrationAction()
                append(
                    when (action.vibrationPattern) {
                        is VibrationAction.VibrationPattern.Short -> context.getString(RBase.string.vibration_short)
                        is VibrationAction.VibrationPattern.Long -> context.getString(RBase.string.vibration_long)
                        else -> getDefaultName()
                    }
                )
                if (action.count > 0) {
                    append(" ")
                    append(action.count)
                }
            }
        }
        BehaviourType.VOICE -> {
            val action = toVoiceAction()
            action.content2.ifBlank {
                action.content.ifBlank { null }
            }
        }
        BehaviourType.BEEP -> {
            val count = toBeepAction().count
            if (count > 0) "${getDefaultName()} $count" else null
        }
        BehaviourType.HALF -> {
            val option = toHalfAction().option
            if (option != HalfAction.OPTION_VOICE) {
                buildString {
                    append(getDefaultName())
                    append(" ")
                    append(
                        when (option) {
                            HalfAction.OPTION_MUSIC -> context.getString(RBase.string.half_option_music)
                            HalfAction.OPTION_VIBRATION -> context.getString(RBase.string.half_option_vibration)
                            else -> ""
                        }
                    )
                }
            } else {
                null
            }
        }
        BehaviourType.COUNT -> {
            val times = toCountAction().times
            if (times != CountAction.DEFAULT_TIMES) "${getDefaultName()} $times" else null
        }
        BehaviourType.NOTIFICATION -> {
            val duration = toNotificationAction().duration
            if (duration != 0) "${getDefaultName()} $duration" else null
        }
        else -> null
    } ?: getDefaultName()
}
