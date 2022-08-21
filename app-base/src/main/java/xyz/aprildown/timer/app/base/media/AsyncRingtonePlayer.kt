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
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import timber.log.Timber
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.tools.helper.getResourceUri
import xyz.aprildown.tools.music.AudioFocusManager
import java.io.IOException
import kotlin.math.pow

internal class AsyncRingtonePlayer(private val mContext: Context) {

    /** Handler running on the ringtone thread.  */
    private val mHandler: Handler by lazy { getNewHandler() }

    private val mPlaybackDelegate = MediaPlayerPlaybackDelegate()

    fun play(
        ringtoneUri: Uri,
        crescendoDuration: Long,
        loop: Boolean,
        audioFocusType: Int,
        streamType: Int
    ) {
        Timber.tag("PLAY_RINGTONE").i(ringtoneUri.toString())
        postMessage(
            messageCode = EVENT_PLAY,
            ringtoneUri = ringtoneUri,
            crescendoDuration = crescendoDuration,
            loop = loop,
            audioFocusType = audioFocusType,
            streamType = streamType,
            delayMillis = 0
        )
    }

    fun stop() {
        postMessage(EVENT_STOP, null, 0, false, 0, 0, 0)
    }

    /** Schedules an adjustment of the playback volume 50ms in the future.  */
    private fun scheduleVolumeAdjustment() {
        // Ensure we never have more than one volume adjustment queued.
        mHandler.removeMessages(EVENT_VOLUME)
        // Queue the next volume adjustment.
        postMessage(EVENT_VOLUME, null, 0, false, 0, 0, 50)
    }

    /**
     * Posts a message to the ringtone-thread handler.
     *
     * @param messageCode the message to post
     * @param ringtoneUri the ringtone in question, if any
     * @param crescendoDuration the length of time, in ms, over which to crescendo the ringtone
     * @param delayMillis the amount of time to delay sending the message, if any
     */
    private fun postMessage(
        messageCode: Int,
        ringtoneUri: Uri?,
        crescendoDuration: Long,
        loop: Boolean,
        audioFocusType: Int,
        streamType: Int,
        delayMillis: Long
    ) {
        synchronized(this) {
            val message = mHandler.obtainMessage(messageCode)
            if (ringtoneUri != null) {
                message.data = bundleOf(
                    RINGTONE_URI_KEY to ringtoneUri,
                    CRESCENDO_DURATION_KEY to crescendoDuration,
                    LOOP to loop,
                    AUDIO_FOCUS_TYPE to audioFocusType,
                    STREAM_TYPE to streamType
                )
            }

            mHandler.sendMessageDelayed(message, delayMillis)
        }
    }

    /**
     * Loops playback of a ringtone using [MediaPlayer].
     */
    private inner class MediaPlayerPlaybackDelegate : AudioManager.OnAudioFocusChangeListener {

        /** The audio focus manager. Only used by the ringtone thread.  */
        private var mAudioManager: AudioManager? = null

        /** Non-`null` while playing a ringtone; `null` otherwise.  */
        private var mMediaPlayer: MediaPlayer? = null

        /** The duration over which to increase the volume.  */
        private var mCrescendoDuration: Long = 0

        /** The time at which the crescendo shall cease; 0 if no crescendo is present.  */
        private var mCrescendoStopTime: Long = 0

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
            crescendoDuration: Long,
            loop: Boolean,
            audioFocusType: Int,
            streamType: Int
        ): Boolean {
            checkAsyncRingtonePlayerThread()
            mCrescendoDuration = crescendoDuration
            mLoop = loop
            mAudioFocusType = audioFocusType
            mStreamType = streamType

            if (mAudioManager == null) {
                mAudioManager = context.getSystemService()
            }

            val inTelephoneCall = isInTelephoneCall(context)
            var alarmNoise: Uri? = ringtoneUri
            // Fall back to the system default alarm if the database does not have an alarm stored.
            if (alarmNoise == null) {
                alarmNoise = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            mMediaPlayer = MediaPlayer()
            mMediaPlayer?.setOnErrorListener { _, _, _ ->
                this@AsyncRingtonePlayer.stop()
                true
            }

            try {
                // If alarmNoise is a custom ringtone on the sd card the app must be granted
                // android.permission.READ_EXTERNAL_STORAGE. Pre-M this is ensured at app
                // installation time. M+, this permission can be revoked by the user any time.
                mMediaPlayer?.setDataSource(context, alarmNoise!!)

                return startPlayback(inTelephoneCall)
            } catch (_: Throwable) {
                // The alarmNoise may be on the sd card which could be busy right now.
                // Use the fallback ringtone.
                try {
                    // Must reset the media player to clear the error state.
                    mMediaPlayer?.reset()
                    mMediaPlayer?.setDataSource(context, getFallbackRingtoneUri(context))
                    return startPlayback(inTelephoneCall)
                } catch (_: Throwable) {
                    // At this point we just don't play anything.
                }
            }
            return false
        }

        /**
         * Prepare the MediaPlayer for playback if the alarm stream is not muted, then start the
         * playback.
         *
         * @param inTelephoneCall `true` if there is currently an active telephone call
         * @return `true` if a crescendo has started and future volume adjustments are
         * required to advance the crescendo effect
         */
        @Throws(IOException::class)
        private fun startPlayback(inTelephoneCall: Boolean): Boolean {
            // Do not play alarms if stream volume is 0 (typically because ringer mode is silent).
            if (mAudioManager?.getStreamVolume(mStreamType) == 0) {
                return false
            }

            // Indicate the ringtone should be played via the alarm stream.
            val audioAttributes = AudioAttributes.Builder()
                .setLegacyStreamType(mStreamType)
                .build()
            mMediaPlayer?.setAudioAttributes(audioAttributes)

            // Check if we are in a call. If we are, use the in-call alarm resource at a low volume
            // to not disrupt the call.
            var scheduleVolumeAdjustment = false
            if (inTelephoneCall) {
                mMediaPlayer?.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME)
            } else if (mCrescendoDuration > 0) {
                mMediaPlayer?.setVolume(0f, 0f)

                // Compute the time at which the crescendo will stop.
                mCrescendoStopTime = now() + mCrescendoDuration
                scheduleVolumeAdjustment = true
            }

            mMediaPlayer?.run {
                isLooping = mLoop
                if (!mLoop) {
                    setOnCompletionListener {
                        this@AsyncRingtonePlayer.stop()
                    }
                }
                prepare()

                mAudioManager?.let {
                    if (mAudioFocusType <= 0) return@let
                    AudioFocusManager.requestAudioFocus(
                        audioManager = it,
                        focusGain = mAudioFocusType,
                        streamType = mStreamType,
                        listener = this@MediaPlayerPlaybackDelegate
                    )
                    becomeNoisyReceiver = BecomeNoisyReceiver()
                    mContext.registerReceiver(
                        becomeNoisyReceiver,
                        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                    )
                }

                start()
            }
            return scheduleVolumeAdjustment
        }

        override fun onAudioFocusChange(focusChange: Int) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    this@AsyncRingtonePlayer.stop()
            }
        }

        /**
         * Stops the playback of the ringtone. Executes on the ringtone-thread.
         */
        fun stop() {
            checkAsyncRingtonePlayerThread()

            mCrescendoDuration = 0
            mCrescendoStopTime = 0

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer?.stop()
                mMediaPlayer?.release()
                mMediaPlayer = null
            }

            mAudioManager?.let {
                AudioFocusManager.abandonAudioFocus(it, this@MediaPlayerPlaybackDelegate)
            }
            if (becomeNoisyReceiver != null) {
                mContext.unregisterReceiver(becomeNoisyReceiver)
                becomeNoisyReceiver = null
            }
        }

        /**
         * Adjusts the volume of the ringtone being played to create a crescendo effect.
         */
        fun adjustVolume(): Boolean {
            checkAsyncRingtonePlayerThread()

            // If media player is absent or not playing, ignore volume adjustment.
            if (mMediaPlayer == null || mMediaPlayer?.isPlaying != true) {
                mCrescendoDuration = 0
                mCrescendoStopTime = 0
                return false
            }

            // If the crescendo is complete set the volume to the maximum; we're done.
            val currentTime = now()
            if (currentTime > mCrescendoStopTime) {
                mCrescendoDuration = 0
                mCrescendoStopTime = 0
                mMediaPlayer?.setVolume(1f, 1f)
                return false
            }

            // The current volume of the crescendo is the percentage of the crescendo completed.
            val volume = computeVolume(currentTime, mCrescendoStopTime, mCrescendoDuration)
            mMediaPlayer?.setVolume(volume, volume)
            // Schedule the next volume bump in the crescendo.
            return true
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
                        if (mPlaybackDelegate.play(
                                context = mContext,
                                ringtoneUri = data.getParcelable(RINGTONE_URI_KEY),
                                crescendoDuration = data.getLong(CRESCENDO_DURATION_KEY),
                                loop = data.getBoolean(LOOP),
                                audioFocusType = data.getInt(AUDIO_FOCUS_TYPE),
                                streamType = data.getInt(STREAM_TYPE)
                            )
                        ) {
                            scheduleVolumeAdjustment()
                        }
                    }
                    EVENT_STOP -> mPlaybackDelegate.stop()
                    EVENT_VOLUME -> if (mPlaybackDelegate.adjustVolume()) {
                        scheduleVolumeAdjustment()
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
private const val EVENT_VOLUME = 3

private const val RINGTONE_URI_KEY = "RINGTONE_URI_KEY"
private const val CRESCENDO_DURATION_KEY = "CRESCENDO_DURATION_KEY"
private const val LOOP = "LOOP"
private const val AUDIO_FOCUS_TYPE = "AUDIO_FOCUS_TYPE"
private const val STREAM_TYPE = "STREAM_TYPE"

/**
 * @return `true` iff the device is currently in a telephone call
 */
private fun isInTelephoneCall(context: Context): Boolean {
    val tm = context.getSystemService<TelephonyManager>()
    return try {
        tm?.callState != TelephonyManager.CALL_STATE_IDLE
    } catch (_: SecurityException) {
        false
    }
}

private fun now(): Long {
    return SystemClock.elapsedRealtime()
}

/**
 * @return Uri of the ringtone to play when the chosen ringtone fails to play
 */
private fun getFallbackRingtoneUri(context: Context): Uri {
    return context.getResourceUri(R.raw.default_ringtone)
}

/**
 * @param currentTime current time of the device
 * @param stopTime time at which the crescendo finishes
 * @param duration length of time over which the crescendo occurs
 * @return the scalar volume value that produces a linear increase in volume (in decibels)
 */
private fun computeVolume(currentTime: Long, stopTime: Long, duration: Long): Float {
    // Compute the percentage of the crescendo that has completed.
    val elapsedCrescendoTime = (stopTime - currentTime).toFloat()
    val fractionComplete = 1 - elapsedCrescendoTime / duration

    // Use the fraction to compute a target decibel between -40dB (near silent) and 0dB (max).
    val gain = fractionComplete * 40 - 40

//            Timber.v("Ringtone crescendo %,.2f%% complete (scalar: %f, volume: %f dB)",
//                    fractionComplete * 100, volume, gain)

    // Convert the target gain (in decibels) into the corresponding volume scalar.
    return 10.0.pow((gain / 20f).toDouble()).toFloat()
}
