package xyz.aprildown.timer.app.base.media

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.core.net.toFile
import java.io.File

private fun <T> withMediaMetadataRetriever(
    setDataSource: MediaMetadataRetriever.() -> Unit,
    block: (retriever: MediaMetadataRetriever) -> T?
): T? {
    var retriever: MediaMetadataRetriever? = null
    return try {
        retriever = MediaMetadataRetriever()
        setDataSource(retriever)
        block(retriever)
    } catch (_: Throwable) {
        // Ignore
        null
    } finally {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                retriever?.close()
            } else {
                retriever?.release()
            }
        } catch (_: Throwable) {
            // Ignore
        }
    }
}

private fun retrieveMediaDuration(setDataSource: MediaMetadataRetriever.() -> Unit): Long {
    return withMediaMetadataRetriever(setDataSource = setDataSource) { retriever ->
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0L
    } ?: 0L
}

fun File.getMediaDuration(): Long {
    return retrieveMediaDuration(setDataSource = { setDataSource(canonicalPath) })
}

fun Uri.getMediaDuration(context: Context): Long {
    if (scheme == ContentResolver.SCHEME_FILE) return toFile().getMediaDuration()
    return retrieveMediaDuration(
        setDataSource = { setDataSource(context, this@getMediaDuration) }
    )
}
