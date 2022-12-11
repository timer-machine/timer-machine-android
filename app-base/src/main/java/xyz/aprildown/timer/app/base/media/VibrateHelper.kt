package xyz.aprildown.timer.app.base.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue

object VibrateHelper {
    private var mVibrator: Vibrator? = null

    private fun getVibrator(context: Context): Vibrator? {
        return mVibrator ?: (context.getSystemService<Vibrator>())?.also {
            mVibrator = it
        }
    }

    fun start(
        context: Context,
        pattern: LongArray,
        repeat: Boolean = true
    ) {
        getVibrator(context)?.run {
            stop(context)
            if (hasVibrator()) {
                val repeatIndex = if (repeat) 1 else -1
                val vibratePattern = longArrayOf(0) + pattern
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                        vibrate(
                            VibrationEffect.createWaveform(vibratePattern, repeatIndex),
                            VibrationAttributes.createForUsage(
                                when (context.storedAudioTypeValue) {
                                    AudioManager.STREAM_ALARM -> VibrationAttributes.USAGE_ALARM
                                    AudioManager.STREAM_NOTIFICATION -> VibrationAttributes.USAGE_NOTIFICATION
                                    else -> VibrationAttributes.USAGE_MEDIA
                                }
                            )
                        )
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        @Suppress("DEPRECATION")
                        vibrate(
                            VibrationEffect.createWaveform(vibratePattern, repeatIndex),
                            AudioAttributes.Builder()
                                .setLegacyStreamType(context.storedAudioTypeValue)
                                .build()
                        )
                    }
                    else -> {
                        @Suppress("DEPRECATION")
                        vibrate(
                            vibratePattern,
                            repeatIndex,
                            AudioAttributes.Builder()
                                .setLegacyStreamType(context.storedAudioTypeValue)
                                .build()
                        )
                    }
                }
            }
        }
    }

    fun stop(context: Context) {
        getVibrator(context)?.cancel()
        mVibrator = null
    }
}
