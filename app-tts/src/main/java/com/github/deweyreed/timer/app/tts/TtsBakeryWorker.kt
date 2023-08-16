package com.github.deweyreed.timer.app.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.aprildown.timer.domain.di.IoDispatcher
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltWorker
internal class TtsBakeryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, params) {
    private var textToSpeech: TextToSpeech? = null
    private val mutex = Mutex()

    private suspend fun getTextToSpeech(): TextToSpeech? {
        if (textToSpeech != null) return textToSpeech
        return mutex.withLock {
            if (textToSpeech != null) return@withLock textToSpeech
            suspendCancellableCoroutine { cont ->
                textToSpeech = TextToSpeech(applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        cont.resume(textToSpeech)
                    } else {
                        textToSpeech = null
                        cont.resume(null)
                    }
                }
            }
        }
    }

    override suspend fun doWork(): Result {
        val data = inputData
        val text =
            data.getString(EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: return Result.success()
        if (TtsBakeryDiskCache.get(applicationContext, text) != null) return Result.success()
        return try {
            synthesize(text)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e)
            Result.retry()
        } finally {
            textToSpeech?.run {
                stop()
                shutdown()
            }
        }
    }

    private suspend fun synthesize(text: String): Unit = withContext(ioDispatcher) {
        val textToSpeech = getTextToSpeech() ?: error("Null TextToSpeech")
        val folder = File(applicationContext.cacheDir, "tts-bakery-temp")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = File(folder, UUID.randomUUID().toString())
        textToSpeech.synthesizeToFile(text, file)
        TtsBakeryDiskCache.put(applicationContext, text, file)
    }

    companion object {
        private const val EXTRA_TEXT = "text"

        fun getData(text: String): Data {
            return Data.Builder()
                .putString(EXTRA_TEXT, text)
                .build()
        }

        private suspend fun TextToSpeech.synthesizeToFile(text: String, file: File) {
            suspendCancellableCoroutine { cont ->
                setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) {
                            cont.resume(Unit)
                        }

                        @Suppress("OVERRIDE_DEPRECATION")
                        override fun onError(utteranceId: String?) {
                            cont.resumeWithException(IllegalStateException("onError $text"))
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            cont.resumeWithException(IllegalStateException("onError $text $errorCode"))
                        }
                    }
                )
                synthesizeToFile(text, Bundle.EMPTY, file, text.hashCode().toString())
            }
        }
    }
}
