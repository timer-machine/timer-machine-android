package xyz.aprildown.timer.app.timer.edit

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.InputType
import androidx.core.net.toUri
import com.github.zawadz88.materialpopupmenu.MaterialPopupMenuBuilder
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioFocusType
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue
import xyz.aprildown.timer.app.base.data.PreferenceData.useVoiceContent2
import xyz.aprildown.timer.app.timer.edit.media.BeepDialog
import xyz.aprildown.timer.app.timer.edit.media.HalfDialog
import xyz.aprildown.timer.app.timer.edit.media.VibrationDialog
import xyz.aprildown.timer.app.timer.edit.media.VoiceDialog
import xyz.aprildown.timer.app.timer.edit.voice.VoiceVariableDialog
import xyz.aprildown.timer.component.key.switchItem
import xyz.aprildown.timer.domain.entities.BeepAction
import xyz.aprildown.timer.domain.entities.CountAction
import xyz.aprildown.timer.domain.entities.HalfAction
import xyz.aprildown.timer.domain.entities.MusicAction
import xyz.aprildown.timer.domain.entities.NotificationAction
import xyz.aprildown.timer.domain.entities.ScreenAction
import xyz.aprildown.timer.domain.entities.VibrationAction
import xyz.aprildown.timer.domain.entities.VoiceAction
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.tools.view.SimpleInputDialog
import xyz.aprildown.timer.app.base.R as RBase

internal fun MaterialPopupMenuBuilder.addMusicItems(
    context: Context,
    action: MusicAction,
    onPickMusicClick: () -> Unit,
    onLoopChanged: (Boolean) -> Unit
) {
    section {
        item {
            label = context.getString(RBase.string.music_pick_ringtone)
            callback = {
                onPickMusicClick.invoke()
            }
        }
        switchItem {
            label = context.getString(RBase.string.music_loop)
            onBind = {
                it.isChecked = action.loop == true
            }
            onCheckedChange = { _, isChecked ->
                onLoopChanged.invoke(isChecked)
            }
        }
    }
}

internal fun MaterialPopupMenuBuilder.addVibrationItems(
    context: Context,
    action: VibrationAction,
    onNewCount: (Int) -> Unit,
    onNewPattern: (VibrationAction.VibrationPattern) -> Unit
) {
    section {
        item {
            label = "${context.getString(RBase.string.vibration_pattern)}: ${
                context.getString(
                    when (action.vibrationPattern) {
                        is VibrationAction.VibrationPattern.Short -> RBase.string.vibration_short
                        is VibrationAction.VibrationPattern.Normal -> RBase.string.vibration_normal
                        is VibrationAction.VibrationPattern.Long -> RBase.string.vibration_long
                    }
                )
            }"
            callback = {
                VibrationDialog(context).showPickPatternDialog(action.vibrationPattern) {
                    onNewPattern.invoke(it)
                }
            }
        }
        item {
            label = "${context.getString(RBase.string.vibration_count)}: ${action.count}"
            callback = {
                VibrationDialog(context).showCountDialog(action.count) {
                    onNewCount.invoke(it)
                }
            }
        }
    }
}

internal fun MaterialPopupMenuBuilder.addScreenItems(
    context: Context,
    action: ScreenAction,
    onFullscreenChanged: (Boolean) -> Unit
) {
    section {
        switchItem {
            label = context.getString(RBase.string.screen_fullscreen)
            onBind = {
                it.isChecked = action.fullScreen == true
            }
            onCheckedChange = { _, isChecked ->
                onFullscreenChanged.invoke(isChecked)
            }
        }
    }
}

internal fun MaterialPopupMenuBuilder.addVoiceItems(
    context: Context,
    action: VoiceAction,
    onVoiceContent: (String) -> Unit,
    onVoice2Content: (String) -> Unit
) {
    section {
        item {
            label = context.getString(RBase.string.voice_content_title)
            callback = {
                if (action.content2.isNotBlank() || context.safeSharedPreference.useVoiceContent2) {
                    VoiceVariableDialog(context).show(
                        initialAction = action,
                        onGet = onVoiceContent,
                        onGet2 = onVoice2Content
                    )
                } else {
                    VoiceDialog(context).requestVoiceContent(
                        initialAction = action,
                        onGet = onVoiceContent,
                        onGet2 = onVoice2Content
                    )
                }
            }
        }
    }
}

internal fun MaterialPopupMenuBuilder.addBeepItems(
    context: Context,
    action: BeepAction,
    onBeepCount: (Int) -> Unit,
    onBeepSound: (Int) -> Unit,
    onRespect: (Boolean) -> Unit
) {
    section {
        item {
            label = "${context.getString(RBase.string.beep_count_title)}: ${action.count}"
            callback = {
                BeepDialog(context).showBeepCountDialog(action.count) {
                    onBeepCount.invoke(it)
                }
            }
        }
        item {
            label = context.getString(RBase.string.beep_sound)
            callback = {
                BeepDialog(context).showBeepPicker(
                    select = action.soundIndex,
                    audioFocusType = context.storedAudioFocusType,
                    streamType = context.storedAudioTypeValue
                ) {
                    onBeepSound.invoke(it)
                }
            }
        }
        switchItem {
            label = context.getString(RBase.string.beep_respect_other)
            onBind = {
                it.isChecked = action.respectOtherSound == true
            }
            onCheckedChange = { _, isChecked ->
                onRespect.invoke(isChecked)
            }
        }
    }
}

internal fun MaterialPopupMenuBuilder.addHalfItems(
    context: Context,
    action: HalfAction,
    onHalfOption: (Int) -> Unit
) {
    section {
        item {
            label = "${context.getString(RBase.string.half_option)}: ${
                when (action.option) {
                    HalfAction.OPTION_VOICE -> context.getString(RBase.string.half_option_voice)
                    HalfAction.OPTION_MUSIC -> context.getString(RBase.string.half_option_music)
                    HalfAction.OPTION_VIBRATION -> context.getString(RBase.string.half_option_vibration)
                    else -> ""
                }
            }"
            callback = {
                HalfDialog(context).showOptionDialog(action.option) {
                    onHalfOption.invoke(it)
                }
            }
        }
    }
}

internal fun MaterialPopupMenuBuilder.addCountItems(
    context: Context,
    action: CountAction,
    onCountTimes: (Int) -> Unit
) {
    section {
        item {
            label = "${context.getString(RBase.string.count_times)}: ${action.times}"
            callback = {
                SimpleInputDialog(context).show(
                    titleRes = RBase.string.count_times,
                    preFill = action.times.toString(),
                    inputType = InputType.TYPE_CLASS_NUMBER,
                    messageRes = RBase.string.count_times_desp
                ) {
                    onCountTimes.invoke(it.toIntOrNull() ?: CountAction.DEFAULT_TIMES)
                }
            }
        }
    }
}

internal fun MaterialPopupMenuBuilder.addNotificationItems(
    context: Context,
    action: NotificationAction,
    onNotificationDuring: (Int) -> Unit
) {
    section {
        item {
            label = "${context.getString(RBase.string.notification_duration)}: ${action.duration}"
            callback = {
                SimpleInputDialog(context).show(
                    titleRes = RBase.string.notification_duration,
                    preFill = action.duration.toString(),
                    inputType = InputType.TYPE_CLASS_NUMBER,
                    messageRes = RBase.string.notification_duration_desp
                ) {
                    onNotificationDuring.invoke(it.toIntOrNull() ?: 0)
                }
            }
        }
        item {
            label = context.getString(RBase.string.notification_settings)
            callback = {
                val settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .putExtra(Settings.EXTRA_CHANNEL_ID, Constants.CHANNEL_B_NOTIF)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setData("package:${context.packageName}".toUri())
                }
                context.startActivity(settingsIntent)
            }
        }
    }
}
