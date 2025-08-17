/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.aprildown.timer.app.base.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.github.deweyreed.tools.helper.getResourceUri
import xyz.aprildown.timer.app.base.R
import java.io.IOException

internal class AsyncRingtonePlayer(private val mContext: Context) {

    /** Handler running on the ringtone thread.  */
    private val mHandler: Handler by lazy { getNewHandler() }

    private val mPlaybackDelegate = ExoPlayerPlaybackDelegate()

    fun play(
        ringtoneUri: Uri,
        loop: Boolean,
        audioFocusType: Int,
        streamType: Int
    ) {
        postMessage(
            messageCode = EVENT_PLAY,
            ringtoneUri = ringtoneUri,
            loop = loop,
            audioFocusType = audioFocusType,
            streamType = streamType,
        )
    }

    fun stop() {
        postMessage(EVENT_STOP, null, false, 0, 0)
    }

    /**
     * Posts a message to the ringtone-thread handler.
     *
     * @param messageCode the message to post
     * @param ringtoneUri the ringtone in question, if any
     */
    private fun postMessage(
        messageCode: Int,
        ringtoneUri: Uri?,
        loop: Boolean,
        audioFocusType: Int,
        streamType: Int,
    ) {
        synchronized(this) {
            val message = mHandler.obtainMessage(messageCode)
            if (ringtoneUri != null) {
                message.data = bundleOf(
                    RINGTONE_URI_KEY to ringtoneUri,
                    LOOP to loop,
                    AUDIO_FOCUS_TYPE to audioFocusType,
                    STREAM_TYPE to streamType
                )
            }

            mHandler.sendMessage(message)
        }
    }

    /**
     * Loops playback of a ringtone using [ExoPlayer].
     */
    private inner class ExoPlayerPlaybackDelegate : AudioManager.OnAudioFocusChangeListener {

        /** The audio focus manager. Only used by the ringtone thread.  */
        private var mAudioManager: AudioManager? = null

        /**
         * Non-`null` while playing a ringtone; `null` otherwise.
         * [android.media.MediaPlayer] doesn't handle internally looping media properly.
         */
        private var mExoPlayer: ExoPlayer? = null

        private var mLoop: Boolean = false

        private var mAudioFocusType: Int = 0
        private var mStreamType: Int = 0

        private var becomeNoisyReceiver: BecomeNoisyReceiver? = null

        /**
         * Starts the actual playback of the ringtone. Executes on ringtone-thread.
         */
        fun play(
            context: Context,
            ringtoneUri: Uri?,
            loop: Boolean,
            audioFocusType: Int,
            streamType: Int
        ) {
            checkAsyncRingtonePlayerThread()
            mLoop = loop
            mAudioFocusType = audioFocusType
            mStreamType = streamType

            if (mAudioManager == null) {
                mAudioManager = context.getSystemService()
            }

            val inTelephoneCall = isInTelephoneCall(context)
            var alarmNoise: Uri? = ringtoneUri
            // Fall back to the system default alarm if the database does not have an alarm stored.
            if (alarmNoise == null || alarmNoise == Uri.EMPTY) {
                alarmNoise = getFallbackRingtoneUri(context)
            }

            mExoPlayer = ExoPlayer.Builder(context).build()
            mExoPlayer?.addListener(
                object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        this@AsyncRingtonePlayer.stop()
                    }
                }
            )

            try {
                // If alarmNoise is a custom ringtone on the sd card the app must be granted
                // android.permission.READ_EXTERNAL_STORAGE. Pre-M this is ensured at app
                // installation time. M+, this permission can be revoked by the user any time.
                mExoPlayer?.setMediaItem(MediaItem.fromUri(alarmNoise))

                startPlayback(inTelephoneCall)
            } catch (_: Throwable) {
                // The alarmNoise may be on the sd card which could be busy right now.
                // Use the fallback ringtone.
                try {
                    // Must reset the media player to clear the error state.
                    mExoPlayer?.stop()
                    mExoPlayer?.setMediaItem(MediaItem.fromUri(getFallbackRingtoneUri(context)))
                    startPlayback(inTelephoneCall)
                } catch (_: Throwable) {
                    // At this point we just don't play anything.
                }
            }
        }

        /**
         * Prepare the player for playback if the alarm stream is not muted, then start the
         * playback.
         *
         * @param inTelephoneCall `true` if there is currently an active telephone call
         * @return `true` if a crescendo has started and future volume adjustments are
         * required to advance the crescendo effect
         */
        @Throws(IOException::class)
        private fun startPlayback(inTelephoneCall: Boolean) {
            // Indicate the ringtone should be played via the alarm stream.
            var contentType = C.AUDIO_CONTENT_TYPE_UNKNOWN
            var usage = C.USAGE_MEDIA
            when (mStreamType) {
                AudioManager.STREAM_ALARM -> {
                    contentType = C.AUDIO_CONTENT_TYPE_SONIFICATION
                    usage = C.USAGE_ALARM
                }
                AudioManager.STREAM_NOTIFICATION -> {
                    contentType = C.AUDIO_CONTENT_TYPE_SONIFICATION
                    usage = C.USAGE_NOTIFICATION
                }
                else -> Unit
            }
            mExoPlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(contentType)
                    .setUsage(usage)
                    .build(),
                false
            )

            // Check if we are in a call. If we are, use the in-call alarm resource at a low volume
            // to not disrupt the call.
            if (inTelephoneCall) {
                mExoPlayer?.volume = IN_CALL_VOLUME
            }

            mExoPlayer?.run {
                repeatMode = if (mLoop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                if (!mLoop) {
                    addListener(
                        object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_ENDED) {
                                    this@AsyncRingtonePlayer.stop()
                                }
                            }
                        }
                    )
                }
                playWhenReady = true
                prepare()

                mAudioManager?.let {
                    if (mAudioFocusType <= 0) return@let
                    AudioFocusManager.requestAudioFocus(
                        audioManager = it,
                        focusGain = mAudioFocusType,
                        streamType = mStreamType,
                        listener = this@ExoPlayerPlaybackDelegate
                    )
                    becomeNoisyReceiver = BecomeNoisyReceiver()
                    ContextCompat.registerReceiver(
                        mContext,
                        becomeNoisyReceiver,
                        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                }
            }
        }

        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    this@AsyncRingtonePlayer.stop()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    try {
                        if (mExoPlayer?.isPlaying == true) {
                            mExoPlayer?.pause()
                        }
                    } catch (_: Throwable) {
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    try {
                        if (mExoPlayer?.isPlaying == false) {
                            mExoPlayer?.play()
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
        }

        /**
         * Stops the playback of the ringtone. Executes on the ringtone-thread.
         */
        fun stop() {
            checkAsyncRingtonePlayerThread()

            // Stop audio playing
            if (mExoPlayer != null) {
                mExoPlayer?.stop()
                mExoPlayer?.release()
                mExoPlayer = null
            }

            mAudioManager?.let {
                AudioFocusManager.abandonAudioFocus(it, this@ExoPlayerPlaybackDelegate)
            }
            if (becomeNoisyReceiver != null) {
                mContext.unregisterReceiver(becomeNoisyReceiver)
                becomeNoisyReceiver = null
            }
        }

        private inner class BecomeNoisyReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    this@AsyncRingtonePlayer.stop()
                }
            }
        }
    }

    private fun getNewHandler(): Handler {
        val thread = HandlerThread("ringtone-player")
        thread.start()
        return object : Handler(thread.looper) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    EVENT_PLAY -> {
                        val data = msg.data
                        mPlaybackDelegate.play(
                            context = mContext,
                            ringtoneUri = BundleCompat.getParcelable(
                                data,
                                RINGTONE_URI_KEY,
                                Uri::class.java
                            ),
                            loop = data.getBoolean(LOOP),
                            audioFocusType = data.getInt(AUDIO_FOCUS_TYPE),
                            streamType = data.getInt(STREAM_TYPE)
                        )
                    }
                    EVENT_STOP -> {
                        mPlaybackDelegate.stop()
                    }
                }
            }
        }
    }

    private fun checkAsyncRingtonePlayerThread() {
        check(Looper.myLooper() == mHandler.looper) {
            "AsyncRingtonePlayer must be on the AsyncRingtonePlayer thread!"
        }
    }
}

// Volume suggested by media team for in-call alarms.
private const val IN_CALL_VOLUME = 0.125f

// Message codes used with the ringtone thread.
private const val EVENT_PLAY = 1
private const val EVENT_STOP = 2

private const val RINGTONE_URI_KEY = "RINGTONE_URI_KEY"
private const val LOOP = "LOOP"
private const val AUDIO_FOCUS_TYPE = "AUDIO_FOCUS_TYPE"
private const val STREAM_TYPE = "STREAM_TYPE"

/**
 * @return `true` iff the device is currently in a telephone call
 */
private fun isInTelephoneCall(context: Context): Boolean {
    val telecomManager = context.getSystemService<TelecomManager>()
    return try {
        @Suppress("MissingPermission")
        telecomManager?.isInCall == true
    } catch (_: SecurityException) {
        false
    }
}

/**
 * @return Uri of the ringtone to play when the chosen ringtone fails to play
 */
private fun getFallbackRingtoneUri(context: Context): Uri {
    return context.getResourceUri(R.raw.default_ringtone)
}
