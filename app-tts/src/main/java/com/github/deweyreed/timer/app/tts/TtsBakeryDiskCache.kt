package com.github.deweyreed.timer.app.tts

import android.content.Context
import android.media.MediaMetadataRetriever
import com.bumptech.glide.disklrucache.DiskLruCache
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.security.MessageDigest

internal object TtsBakeryDiskCache {
    private lateinit var diskLruCache: DiskLruCache

    private fun getDiskLruCache(context: Context): DiskLruCache {
        if (!::diskLruCache.isInitialized) {
            diskLruCache = DiskLruCache.open(
                File(context.cacheDir, "tts-bakery"),
                1,
                1,
                100 * 1024 * 1024
            )
        }
        return diskLruCache
    }

    fun get(context: Context, text: String): File? {
        val key = getSpeechKey(text)
        var result: File? = null
        try {
            result = getDiskLruCache(context).get(key)?.getFile(0)
        } catch (e: IOException) {
            Timber.e(e)
        }
        if (result != null && !isValidTtsFile(result)) {
            delete(context, text)
            result = null
        }
        return result
    }

    private fun isValidTtsFile(file: File): Boolean {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.canonicalPath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
        } catch (_: Exception) {
            false
        }
    }

    fun put(context: Context, text: String, file: File): Unit = synchronized(this) {
        if (!isValidTtsFile(file)) {
            file.delete()
            error("Invalid TTS file")
        }

        val key = getSpeechKey(text)
        try {
            val editor = getDiskLruCache(context).edit(key)
                ?: error("Null editor")
            try {
                val cacheFile = editor.getFile(0)
                file.copyTo(cacheFile)
                editor.commit()
            } finally {
                editor.abortUnlessCommitted()
            }
        } catch (e: IOException) {
            Timber.e(e)
        } finally {
            file.delete()
        }
    }

    private fun delete(context: Context, text: String) {
        val key = getSpeechKey(text)
        try {
            getDiskLruCache(context).remove(key)
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    // From Glide SafeKeyGenerator
    private fun getSpeechKey(text: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(text.encodeToByteArray())
        val bytes = messageDigest.digest()

        val hexCharArray = "0123456789abcdef".toCharArray()
        val hexChars = CharArray(64)
        var v: Int
        for (j in bytes.indices) {
            v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexCharArray[v ushr 4]
            hexChars[j * 2 + 1] = hexCharArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
