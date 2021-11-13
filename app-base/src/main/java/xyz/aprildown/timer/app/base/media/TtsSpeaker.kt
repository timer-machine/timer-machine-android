package xyz.aprildown.timer.app.base.media

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import timber.log.Timber
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioFocusType
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue
import xyz.aprildown.timer.app.base.data.PreferenceData.useBakedCount
import xyz.aprildown.tools.anko.longToast
import xyz.aprildown.tools.helper.HandlerHelper
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.tools.music.AudioFocusManager
import java.io.File
import xyz.aprildown.timer.app.base.R as RBase

/**
 * It handles states itself.
 */
object TtsSpeaker : AudioManager.OnAudioFocusChangeListener {

    abstract class Callback {
        open fun onDone() = Unit
        open fun onError() = Unit
    }

    private var tts: TextToSpeech? = null

    // We handle it carefully.
    @SuppressLint("StaticFieldLeak")
    private var ttsListener: ProgressListener? = null
    private var shutDownTtsHandler: Handler? = null
    private var am: AudioManager? = null

    fun speak(
        context: Context,
        content: CharSequence,
        sayMore: Boolean = false,
        callback: Callback? = null
    ) {
        Timber.tag("TTS_SPEAK").i("\"$content\"")
        val appContext = context.applicationContext

        if (tryBakedCount(context, content)) return

        val streamType = appContext.storedAudioTypeValue
        // We don't need to shut down now.
        shutDownTtsHandler?.removeCallbacksAndMessages(null)
        if (tts == null) {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    beginSpeaking(content, streamType)
                } else {
                    stopSpeaking()
                    shutDownTts(immediate = true)
                    abandonAudioFocus(immediate = true)

                    HandlerHelper.runOnUiThread {
                        context.longToast(
                            context.getString(RBase.string.tts_error_template, status.toString())
                        )
                        callback?.onError()
                    }
                }
            }.apply {
                ttsListener = ProgressListener(
                    context = appContext,
                    audioFocusType = appContext.storedAudioFocusType,
                    streamType = streamType,
                    sayMore = sayMore,
                    callback = callback
                )
                setOnUtteranceProgressListener(ttsListener)
            }
        } else {
            val listener = ttsListener
            // Its lifecycle should be same as tts.
            requireNotNull(listener)
            listener.sayMore = sayMore
            listener.streamType = streamType
            listener.callback = callback
            beginSpeaking(content, streamType)
        }
    }

    /**
     * @return If the [content] is consumed by the baked count.
     */
    private fun tryBakedCount(context: Context, content: CharSequence): Boolean {
        if (content.length > 2) return false
        if ((content.toString().toIntOrNull() ?: -1) !in 0..20) return false
        if (!context.safeSharedPreference.useBakedCount) return false

        val folder = File(context.filesDir, PreferenceData.BAKED_COUNT_NAME)
        val file = File(folder, "$content.mp3")
        if (!file.exists()) return false

        val audioFocusType = context.storedAudioFocusType
        val streamType = context.storedAudioTypeValue

        requestAudioFocus(context, audioFocusType, streamType)

        RingtonePreviewKlaxon.start(
            context = context,
            uri = file.toUri(),
            crescendoDuration = 0L,
            loop = false,
            audioFocusType = 0, // AudioManager.AUDIOFOCUS_NONE
            streamType = streamType
        )

        return true
    }

    private class ProgressListener(
        val context: Context,
        val audioFocusType: Int,
        var streamType: Int,
        var sayMore: Boolean,
        var callback: Callback?
    ) : UtteranceProgressListener() {

        override fun onStart(utteranceId: String?) {
            HandlerHelper.runOnUiThread {
                requestAudioFocus(
                    context = context,
                    audioFocusType = audioFocusType,
                    streamType = streamType
                )
            }
        }

        override fun onDone(utteranceId: String?) {
            HandlerHelper.runOnUiThread {
                if (!sayMore) {
                    stopSpeaking()
                    shutDownTts(immediate = false)
                    abandonAudioFocus(immediate = false)
                }
                callback?.onDone()
            }
        }

        override fun onError(utteranceId: String?) {
            onError(utteranceId, TextToSpeech.ERROR)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            HandlerHelper.runOnUiThread {
                stopSpeaking()
                shutDownTts(immediate = true)
                abandonAudioFocus(immediate = true)
                context.longToast(
                    context.getString(RBase.string.tts_error_template, errorCode.toString())
                )
                callback?.onError()
            }
        }
    }

    private fun beginSpeaking(
        content: CharSequence,
        streamType: Int = AudioManager.STREAM_MUSIC
    ) {
        val textToSpeech = tts
        requireNotNull(textToSpeech)

        if (content.isBlank()) return

        if (textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }

        textToSpeech.speak(
            content,
            TextToSpeech.QUEUE_FLUSH,
            bundleOf(
                TextToSpeech.Engine.KEY_PARAM_STREAM to streamType
            ),
            content.hashCode().toString()
        )
    }

    /**
     * Stop and clean [TtsSpeaker].
     * This method might be called many times. So it batches all calls by default.
     * @param force true if you wish to force shutdown and abandon all batches.
     */
    fun tearDown(force: Boolean = false) {
        stopSpeaking()
        shutDownTts(immediate = force)
        abandonAudioFocus(immediate = force)
    }

    private fun stopSpeaking() {
        tts?.stop()
    }

    private fun shutDownTts(immediate: Boolean = false) {
        // If tts is null, don't even think about it.
        if (tts == null) return

        fun run() {
            tts?.run {
                shutdown()
                tts = null
                ttsListener = null
                shutDownTtsHandler?.removeCallbacksAndMessages(null)
                shutDownTtsHandler = null
            }
        }

        if (immediate) {
            run()
        } else {
            // Later message has higher priority.
            shutDownTtsHandler?.removeCallbacksAndMessages(null)
            (shutDownTtsHandler ?: Handler(Looper.getMainLooper()).also {
                shutDownTtsHandler = it
            }).postDelayed(5_000L) {
                run()
            }
        }
    }

    private fun requestAudioFocus(
        context: Context,
        audioFocusType: Int,
        streamType: Int
    ) {
        (am ?: context.getSystemService<AudioManager>()?.also { am = it })?.let {
            AudioFocusManager.requestAudioFocus(
                audioManager = it,
                focusGain = audioFocusType,
                streamType = streamType,
                listener = this
            )
        }
    }

    private fun abandonAudioFocus(immediate: Boolean = false) {
        am?.let {

            fun run() {
                AudioFocusManager.abandonAudioFocus(it, this@TtsSpeaker)
                am = null
            }

            if (immediate) {
                run()
            } else {
                HandlerHelper.postDelayed(512) {
                    run()
                }
            }
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> tearDown()
        }
    }
}
