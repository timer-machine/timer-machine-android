package com.github.deweyreed.timer.component.tts

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.io.File

object TtsBakery {
    fun getSpeechFile(context: Context, text: String): File? {
        return TtsBakeryDiskCache.get(context, text)
    }

    fun scheduleBaking(context: Context, text: String) {
        if (text.isBlank()) return
        WorkManager.getInstance(context)
            .enqueue(
                OneTimeWorkRequest.Builder(TtsBakeryWorker::class.java)
                    .setInputData(TtsBakeryWorker.getData(text))
                    .setConstraints(
                        Constraints(
                            requiredNetworkType = NetworkType.CONNECTED,
                            requiresBatteryNotLow = true,
                            requiresStorageNotLow = true,
                        )
                    )
                    .build()
            )
    }

    fun tearDown(context: Context) {
        TtsBakeryDiskCache.deleteAll(context)
    }
}
