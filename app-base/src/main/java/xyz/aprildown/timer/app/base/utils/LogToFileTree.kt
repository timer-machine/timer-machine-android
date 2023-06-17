package xyz.aprildown.timer.app.base.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.domain.utils.fireAndForget
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class LogToFileTree(private val context: Context) : Timber.Tree() {

    private val logMutex = Mutex()
    private val cachedMessage = mutableListOf<String>()
    private val messageLock = Any()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        synchronized(messageLock) {
            cachedMessage += message
        }

        fireAndForget(Dispatchers.IO) {
            logMutex.withLock {
                try {
                    val messageToLog = synchronized(messageLock) {
                        if (cachedMessage.isEmpty()) return@withLock
                        cachedMessage.removeAt(0)
                    }

                    val logFile = File(context.filesDir, Constants.FILENAME_RUNNING_LOG)

                    when {
                        !logFile.exists() -> {
                            logFile.createNewFile()
                        }
                        logFile.length() > 10 * 1024 * 1024L -> { // MB KB B
                            logFile.delete()
                            logFile.createNewFile()
                        }
                    }

                    logFile.appendText(
                        buildString {
                            append(
                                SimpleDateFormat("yyyy-MM-dd-kk-mm-ss", Locale.getDefault())
                                    .format(System.currentTimeMillis())
                            )
                            append(" ")
                            append(if (tag.isNullOrBlank()) "" else "$tag ")
                            append(messageToLog)
                            append("\n")
                        }
                    )
                } catch (_: Exception) {
                }
            }
        }
    }
}
