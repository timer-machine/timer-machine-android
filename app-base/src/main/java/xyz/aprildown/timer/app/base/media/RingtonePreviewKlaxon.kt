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

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri

object RingtonePreviewKlaxon {

    @SuppressLint("StaticFieldLeak")
    private lateinit var sAsyncRingtonePlayer: AsyncRingtonePlayer

    fun stop(context: Context) {
        getAsyncRingtonePlayer(context).stop()
    }

    fun start(
        context: Context,
        uri: Uri,
        crescendoDuration: Long,
        loop: Boolean,
        audioFocusType: Int,
        streamType: Int
    ) {
        stop(context)
        getAsyncRingtonePlayer(context).play(
            ringtoneUri = uri,
            crescendoDuration = crescendoDuration,
            loop = loop,
            audioFocusType = audioFocusType,
            streamType = streamType
        )
    }

    @Synchronized
    private fun getAsyncRingtonePlayer(context: Context): AsyncRingtonePlayer {
        if (!::sAsyncRingtonePlayer.isInitialized) {
            sAsyncRingtonePlayer = AsyncRingtonePlayer(context.applicationContext)
        }

        return sAsyncRingtonePlayer
    }
}
