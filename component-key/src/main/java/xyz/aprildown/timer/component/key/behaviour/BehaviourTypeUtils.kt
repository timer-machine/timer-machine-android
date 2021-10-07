package xyz.aprildown.timer.component.key.behaviour

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import xyz.aprildown.timer.component.key.R
import xyz.aprildown.timer.domain.entities.BehaviourType

val BehaviourType.iconRes: Int
    @DrawableRes
    get() = when (this) {
        BehaviourType.MUSIC -> R.drawable.ic_music
        BehaviourType.VIBRATION -> R.drawable.ic_vibration
        BehaviourType.SCREEN -> R.drawable.ic_screen
        BehaviourType.HALT -> R.drawable.ic_halt
        BehaviourType.VOICE -> R.drawable.ic_voice
        BehaviourType.BEEP -> R.drawable.ic_beep
        BehaviourType.HALF -> R.drawable.ic_half
        BehaviourType.COUNT -> R.drawable.ic_count
        BehaviourType.NOTIFICATION -> R.drawable.ic_notification
        BehaviourType.FLASHLIGHT -> R.drawable.ic_flashlight
    }

val BehaviourType.nameRes: Int
    @StringRes
    get() = when (this) {
        BehaviourType.MUSIC -> R.string.behaviour_music
        BehaviourType.VIBRATION -> R.string.behaviour_vibration
        BehaviourType.SCREEN -> R.string.behaviour_screen
        BehaviourType.HALT -> R.string.behaviour_halt
        BehaviourType.VOICE -> R.string.behaviour_voice
        BehaviourType.BEEP -> R.string.behaviour_beep
        BehaviourType.HALF -> R.string.behaviour_half
        BehaviourType.COUNT -> R.string.behaviour_count
        BehaviourType.NOTIFICATION -> R.string.behaviour_notification
        BehaviourType.FLASHLIGHT -> R.string.behaviour_flashlight
    }

val BehaviourType.despRes: Int
    @StringRes
    get() = when (this) {
        BehaviourType.MUSIC -> R.string.behaviour_music_help
        BehaviourType.VIBRATION -> R.string.behaviour_vibration_help
        BehaviourType.SCREEN -> R.string.behaviour_screen_help
        BehaviourType.HALT -> R.string.behaviour_halt_help
        BehaviourType.VOICE -> R.string.behaviour_voice_help
        BehaviourType.BEEP -> R.string.behaviour_beep_help
        BehaviourType.HALF -> R.string.behaviour_half_help
        BehaviourType.COUNT -> R.string.behaviour_count_help
        BehaviourType.NOTIFICATION -> R.string.behaviour_notification_help
        BehaviourType.FLASHLIGHT -> R.string.behaviour_flashlight_help
    }
