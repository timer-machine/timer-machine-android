package xyz.aprildown.timer.domain.utils

import android.graphics.Bitmap
import android.os.Build
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream

object Base64BitmapConverter {
    const val PREFIX = "base64,"

    fun Bitmap.encodeToBase64(quality: Int = 100): String {
        val outputStream = ByteArrayOutputStream()
        compress(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            },
            quality,
            outputStream
        )
        val byteArray = outputStream.toByteArray()
        return "$PREFIX${byteArray.toByteString().base64()}"
    }

    fun String.decodeBitmapByteArrayFromBase64(): ByteArray {
        return checkNotNull(substringAfter(PREFIX).decodeBase64()).toByteArray()
    }

    // fun String.decodeBitmapFromBase64(): Bitmap {
    //     val data = decodeBitmapByteArrayFromBase64()
    //     return BitmapFactory.decodeByteArray(data, 0, data.size)
    // }
}
