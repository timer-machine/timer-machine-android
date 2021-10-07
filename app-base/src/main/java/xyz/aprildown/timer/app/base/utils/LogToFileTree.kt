package xyz.aprildown.timer.app.base.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.aprildown.timer.domain.utils.Constants
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class LogToFileTree(private val context: Context) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val logFile = File(context.filesDir, Constants.FILENAME_RUNNING_LOG)

                when {
                    !logFile.exists() -> {
                        logFile.createNewFile()
                    }
                    logFile.length() > 10 /* MB */ * 1024 /* KB */ * 1024L /* B */ -> {
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
                        append(priority)
                        append(" ")
                        append(if (tag.isNullOrBlank()) "" else "$tag ")
                        append(message)
                        append("\n")
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
