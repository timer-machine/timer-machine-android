package xyz.aprildown.timer.app.base.media

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
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

        fun createVibrateAutoAttributes(): AudioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(context.storedAudioTypeValue)
            .build()

        getVibrator(context)?.run {
            stop(context)
            if (hasVibrator()) {
                val repeatIndex = if (repeat) 1 else -1
                val vibratePattern = longArrayOf(0) + pattern
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrate(
                        VibrationEffect.createWaveform(vibratePattern, repeatIndex),
                        createVibrateAutoAttributes()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrate(vibratePattern, repeatIndex, createVibrateAutoAttributes())
                }
            }
        }
    }

    fun stop(context: Context) {
        getVibrator(context)?.cancel()
        mVibrator = null
    }
}
