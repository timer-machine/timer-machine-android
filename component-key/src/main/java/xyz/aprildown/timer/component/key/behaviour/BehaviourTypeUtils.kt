package xyz.aprildown.timer.component.key.behaviour

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.app.base.R as RBase

val BehaviourType.iconRes: Int
    @DrawableRes
    get() = when (this) {
        BehaviourType.MUSIC -> RBase.drawable.ic_music
        BehaviourType.VIBRATION -> RBase.drawable.ic_vibration
        BehaviourType.SCREEN -> RBase.drawable.ic_screen
        BehaviourType.HALT -> RBase.drawable.ic_halt
        BehaviourType.VOICE -> RBase.drawable.ic_voice
        BehaviourType.BEEP -> RBase.drawable.ic_beep
        BehaviourType.HALF -> RBase.drawable.ic_half
        BehaviourType.COUNT -> RBase.drawable.ic_count
        BehaviourType.NOTIFICATION -> RBase.drawable.ic_notification
        BehaviourType.FLASHLIGHT -> RBase.drawable.ic_flashlight
        BehaviourType.IMAGE -> RBase.drawable.ic_image
    }

val BehaviourType.nameRes: Int
    @StringRes
    get() = when (this) {
        BehaviourType.MUSIC -> RBase.string.behaviour_music
        BehaviourType.VIBRATION -> RBase.string.behaviour_vibration
        BehaviourType.SCREEN -> RBase.string.behaviour_screen
        BehaviourType.HALT -> RBase.string.behaviour_halt
        BehaviourType.VOICE -> RBase.string.behaviour_voice
        BehaviourType.BEEP -> RBase.string.behaviour_beep
        BehaviourType.HALF -> RBase.string.behaviour_half
        BehaviourType.COUNT -> RBase.string.behaviour_count
        BehaviourType.NOTIFICATION -> RBase.string.behaviour_notification
        BehaviourType.FLASHLIGHT -> RBase.string.behaviour_flashlight
        BehaviourType.IMAGE -> RBase.string.behaviour_image
    }

val BehaviourType.despRes: Int
    @StringRes
    get() = when (this) {
        BehaviourType.MUSIC -> RBase.string.behaviour_music_help
        BehaviourType.VIBRATION -> RBase.string.behaviour_vibration_help
        BehaviourType.SCREEN -> RBase.string.behaviour_screen_help
        BehaviourType.HALT -> RBase.string.behaviour_halt_help
        BehaviourType.VOICE -> RBase.string.behaviour_voice_help
        BehaviourType.BEEP -> RBase.string.behaviour_beep_help
        BehaviourType.HALF -> RBase.string.behaviour_half_help
        BehaviourType.COUNT -> RBase.string.behaviour_count_help
        BehaviourType.NOTIFICATION -> RBase.string.behaviour_notification_help
        BehaviourType.FLASHLIGHT -> RBase.string.behaviour_flashlight_help
        BehaviourType.IMAGE -> RBase.string.behaviour_image_help
    }
