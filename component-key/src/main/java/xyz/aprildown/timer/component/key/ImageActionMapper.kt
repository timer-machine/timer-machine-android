package xyz.aprildown.timer.component.key

import androidx.core.net.toUri
import coil.map.Mapper
import coil.request.Options
import xyz.aprildown.timer.domain.entities.ImageAction
import xyz.aprildown.timer.domain.entities.ResourceContentType
import xyz.aprildown.timer.domain.utils.Base64BitmapConverter.decodeBitmapByteArrayFromBase64
import java.io.File
import java.nio.ByteBuffer

class ImageActionMapper : Mapper<ImageAction, Any> {
    override fun map(data: ImageAction, options: Options): Any? {
        return when (data.type) {
            ResourceContentType.CanonicalPath -> File(data.data)
            ResourceContentType.RelativePath -> error("Relative path can't be loaded")
            ResourceContentType.Uri -> data.data.toUri()
            ResourceContentType.Base64 -> {
                ByteBuffer.wrap(data.data.decodeBitmapByteArrayFromBase64())
            }
        }
    }
}
