package xyz.aprildown.timer.app.base.media

import android.media.AudioManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import java.util.WeakHashMap

object AudioFocusManager {
    private val sListenerRequestMap =
        WeakHashMap<AudioManager.OnAudioFocusChangeListener, AudioFocusRequestCompat>()

    fun requestAudioFocus(
        audioManager: AudioManager,
        focusGain: Int,
        streamType: Int,
        listener: AudioManager.OnAudioFocusChangeListener
    ) {
        if (focusGain == 0) return
        val request = AudioFocusRequestCompat.Builder(focusGain)
            .setOnAudioFocusChangeListener(listener)
            .setWillPauseWhenDucked(false)
            .setAudioAttributes(
                AudioAttributesCompat.Builder().setLegacyStreamType(streamType).build()
            )
            .build()
        sListenerRequestMap[listener] = request
        AudioManagerCompat.requestAudioFocus(
            audioManager,
            request
        )
    }

    fun abandonAudioFocus(
        audioManager: AudioManager,
        listener: AudioManager.OnAudioFocusChangeListener
    ) {
        val request = sListenerRequestMap.remove(listener)
        if (request != null) {
            AudioManagerCompat.abandonAudioFocusRequest(audioManager, request)
        }
    }
}
