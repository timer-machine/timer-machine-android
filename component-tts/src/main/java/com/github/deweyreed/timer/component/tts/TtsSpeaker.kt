package com.github.deweyreed.timer.component.tts

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.format.DateUtils
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import com.github.deweyreed.tools.anko.longToast
import com.github.deweyreed.tools.helper.HandlerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.data.PreferenceData.isTtsBakeryOpen
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioFocusType
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue
import xyz.aprildown.timer.app.base.data.PreferenceData.useBakedCount
import xyz.aprildown.timer.app.base.media.AudioFocusManager
import xyz.aprildown.timer.app.base.media.RingtonePreviewKlaxon
import xyz.aprildown.timer.domain.utils.fireAndForget
import xyz.aprildown.tools.helper.safeSharedPreference
import java.io.File

object TtsSpeaker : WelcomingTextToSpeech.Listener, AudioManager.OnAudioFocusChangeListener {

    private var application: Application? = null

    private var textToSpeech: WelcomingTextToSpeech? = null

    private var oneShot = false
    private var onDone: (() -> Unit)? = null

    private var audioManager: AudioManager? = null
    private var audioManagerCleanHandler: Handler? = null

    private var nullableCleanHandler: Handler? = null
    private val cleanHandler: Handler
        get() {
            var nch = nullableCleanHandler
            if (nch == null) {
                nch = Handler(Looper.getMainLooper())
                nullableCleanHandler = nch
            }
            return nch
        }

    private fun warmUp(context: Context) {
        var application = application
        if (application == null) {
            application = context.applicationContext as Application
            this.application = application
        }

        cancelScheduledClean()

        var tts = textToSpeech
        if (tts == null) {
            tts = WelcomingTextToSpeech(application = application, listener = this)
            textToSpeech = tts
        }
    }

    fun speak(
        context: Context,
        text: CharSequence,
        oneShot: Boolean,
        onDone: (() -> Unit)? = null
    ) {
        warmUp(context)

        if (text.isNotBlank()) {
            this.oneShot = oneShot
            this.onDone = onDone

            checkNotNull(textToSpeech).speak(text, checkNotNull(application).storedAudioTypeValue)
        }
    }

    fun stopCurrentSpeaking() {
        textToSpeech?.stop()

        oneShot = false
        onDone = null

        abandonAudioFocus()

        scheduleClean()
    }

    override fun onError(errorCode: Int) {
        application?.run {
            longToast(getString(R.string.tts_error_template, errorCode.toString()))
        }
        onDone?.invoke()

        textToSpeech?.run {
            stop()
            textToSpeech = null
        }
        oneShot = false
        onDone = null
        application = null

        abandonAudioFocus()
    }

    override fun onStart() {
        if (audioManager != null) return
        val context = application ?: return
        requestAudioFocus(
            context = context,
            audioFocusType = context.storedAudioFocusType,
            streamType = context.storedAudioTypeValue,
        )
    }

    /**
     * When we use baked count, only [onStart] is called to request the audio focus.
     * onDone isn't called, but it's okay because
     * 1. It's not [oneShot]. 2. It has no [onDone] action. 3. We call [scheduleClean] eventually.
     */
    override fun onDone() {
        if (oneShot) {
            oneShot = false

            abandonAudioFocus()
        }

        onDone?.invoke()
        onDone = null

        scheduleClean()
    }

    private fun requestAudioFocus(
        context: Context,
        audioFocusType: Int,
        streamType: Int
    ) {
        var am = audioManager
        if (am == null) {
            am = context.getSystemService() ?: return
            audioManager = am
        }
        audioManagerCleanHandler?.removeCallbacksAndMessages(null)
        audioManagerCleanHandler = null
        AudioFocusManager.requestAudioFocus(
            audioManager = am,
            focusGain = audioFocusType,
            streamType = streamType,
            listener = this,
        )
    }

    private fun abandonAudioFocus() {
        var handler = audioManagerCleanHandler
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
            audioManagerCleanHandler = handler
        } else {
            handler.removeCallbacksAndMessages(null)
        }

        handler.postDelayed(500) {
            val am = audioManager ?: return@postDelayed
            AudioFocusManager.abandonAudioFocus(audioManager = am, listener = this)
            audioManager = null
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                textToSpeech?.stop()
            }
        }
    }

    private fun scheduleClean() {
        if (textToSpeech == null && onDone == null && audioManager == null) return
        cancelScheduledClean()
        cleanHandler.postDelayed(DateUtils.MINUTE_IN_MILLIS) {
            clean()
        }
    }

    fun clean() {
        cancelScheduledClean()

        textToSpeech?.run {
            stop()
            shutdown()
            textToSpeech = null
        }
        oneShot = false
        onDone = null
        application = null

        abandonAudioFocus()

        nullableCleanHandler = null
    }

    private fun cancelScheduledClean() {
        if (nullableCleanHandler == null) return
        cleanHandler.removeCallbacksAndMessages(null)
    }
}

private class WelcomingTextToSpeech(
    private val application: Application,
    private val listener: Listener,
) : TextToSpeech.OnInitListener {

    interface Listener {
        fun onError(errorCode: Int)
        fun onStart()
        fun onDone()
    }

    private val textToSpeech = TextToSpeech(application, this).also { tts ->
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    HandlerHelper.runOnUiThread {
                        listener.onStart()
                    }
                }

                override fun onDone(utteranceId: String?) {
                    HandlerHelper.runOnUiThread {
                        listener.onDone()
                    }
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String?) {
                    onErrorCompat(TextToSpeech.ERROR)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onErrorCompat(errorCode)
                }

                private fun onErrorCompat(errorCode: Int) {
                    HandlerHelper.runOnUiThread {
                        listener.onError(errorCode)
                    }
                }
            }
        )
    }

    private var initialized = false
    private var pendingText: CharSequence? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            // onInit may be called in the constructor of TextToSpeech
            HandlerHelper.post {
                stop()
                listener.onError(status)
            }
            return
        }
        initialized = true

        val currentPendingText = pendingText
        if (currentPendingText != null) {
            speak(currentPendingText)
        }
    }

    fun speak(text: CharSequence, streamType: Int = AudioManager.STREAM_MUSIC) {
        if (text.isBlank()) return

        fireAndForget(Dispatchers.Main.immediate) {
            val isTtsBakeryOpen = application.safeSharedPreference.isTtsBakeryOpen

            val speechUri = withContext(Dispatchers.IO) {
                getBakedCountUri(context = application, content = text)
                    ?: if (isTtsBakeryOpen) {
                        TtsBakery.getSpeechFile(application, text.toString())?.toUri()
                    } else {
                        null
                    }
            }
            if (speechUri != null) {
                if (initialized) {
                    if (textToSpeech.isSpeaking) {
                        textToSpeech.stop()
                    }
                }

                RingtonePreviewKlaxon.start(
                    context = application,
                    uri = speechUri,
                    crescendoDuration = 0L,
                    loop = false,
                    audioFocusType = 0, // AudioManager.AUDIOFOCUS_NONE
                    streamType = streamType
                )

                listener.onStart()
                return@fireAndForget
            }

            if (!initialized) {
                pendingText = text
                return@fireAndForget
            }

            textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                bundleOf(
                    TextToSpeech.Engine.KEY_PARAM_STREAM to streamType
                ),
                text.hashCode().toString()
            )
            if (isTtsBakeryOpen) {
                TtsBakery.scheduleBaking(application, text.toString())
            }
        }
    }

    fun stop() {
        textToSpeech.stop()
    }

    fun shutdown() {
        textToSpeech.shutdown()
    }
}

private fun getBakedCountUri(context: Context, content: CharSequence): Uri? {
    if (content.length > 2) return null
    if ((content.toString().toIntOrNull() ?: -1) !in 0..20) return null
    if (!context.safeSharedPreference.useBakedCount) return null

    val folder = File(context.filesDir, PreferenceData.BAKED_COUNT_NAME)
    val file = File(folder, "$content.mp3")
    if (!file.exists()) return null

    return file.toUri()
}
