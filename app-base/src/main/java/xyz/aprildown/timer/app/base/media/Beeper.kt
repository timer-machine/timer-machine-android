package xyz.aprildown.timer.app.base.media

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.core.content.getSystemService

object Beeper : AudioManager.OnAudioFocusChangeListener {

    class Settings(
        val audioFocusType: Int,
        val streamType: Int,
        val sound: Int = 1,
        var count: Int = 0,
        val respectOther: Boolean = true
    ) {
        init {
            if (count == 0) count = Int.MAX_VALUE
        }
    }

    private var audioManager: AudioManager? = null
    private var toneGenerator: CustomToneGenerator? = null
    private var toneSettings: Settings? = null
    var isLoaded: Boolean = false
        private set

    fun load(settings: Settings, debounce: Boolean = true) {
        tearDown()
        toneSettings = settings
        toneGenerator = if (debounce) {
            DebounceToneGenerator(settings.streamType)
        } else {
            NormalToneGenerator(settings.streamType)
        }
        isLoaded = true
    }

    fun play(
        context: Context,
        newTone: Int = 0
    ) {
        if (!isLoaded) return

        val settings = toneSettings
        val generator = toneGenerator
        if (settings == null || generator == null) {
            tearDown()
            return
        }

        if (audioManager == null && settings.respectOther) {
            initAudioManagerAndRequestFocus(
                context = context,
                audioFocusType = settings.audioFocusType,
                streamType = settings.streamType
            )
        }

        if (settings.count > 0) {
            if (generator.play(if (newTone == 0) settings.sound else newTone)) {
                --settings.count
            }
        } else {
            tearDown()
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                tearDown()
            }
        }
    }

    fun tearDown() {
        audioManager?.let {
            // If it isn't null, it means we must respect others.
            AudioFocusManager.abandonAudioFocus(it, this)
            audioManager = null
        }

        toneGenerator?.run {
            destroy()
            toneGenerator = null
        }

        toneSettings = null

        isLoaded = false
    }

    private fun initAudioManagerAndRequestFocus(
        context: Context,
        audioFocusType: Int,
        streamType: Int
    ) {
        var am = audioManager
        if (am == null) {
            am = context.getSystemService() ?: return
            audioManager = am
        }
        AudioFocusManager.requestAudioFocus(
            audioManager = am,
            focusGain = audioFocusType,
            streamType = streamType,
            listener = this
        )
    }

    private abstract class CustomToneGenerator(streamType: Int) {
        private val toneGenerator = ToneGenerator(streamType, 100)

        protected fun playTone(tone: Int) {
            toneGenerator.startTone(tone, 100)
        }

        /**
         * @return If the toneGenerator played
         */
        abstract fun play(tone: Int): Boolean

        fun destroy() {
            toneGenerator.run {
                stopTone()
                release()
            }
        }
    }

    private class NormalToneGenerator(streamType: Int) : CustomToneGenerator(streamType) {
        override fun play(tone: Int): Boolean {
            playTone(tone)
            return true
        }
    }

    private class DebounceToneGenerator(streamType: Int) : CustomToneGenerator(streamType) {
        private var lastToneTime = 0L

        override fun play(tone: Int): Boolean {
            val now = SystemClock.elapsedRealtime()
            val elapsed = now - lastToneTime
            if (elapsed >= 750L) {
                lastToneTime = now
                playTone(tone)
                return true
            }
            return false
        }
    }
}
