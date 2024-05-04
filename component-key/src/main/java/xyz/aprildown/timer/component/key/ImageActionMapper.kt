package xyz.aprildown.timer.component.key

import androidx.core.net.toUri
import coil.map.Mapper
import coil.request.Options
import xyz.aprildown.timer.domain.entities.ImageAction
import xyz.aprildown.timer.domain.entities.ResourceContentType
import java.io.File

class ImageActionMapper : Mapper<ImageAction, Any> {
    override fun map(data: ImageAction, options: Options): Any {
        return when (data.type) {
            ResourceContentType.CanonicalPath -> File(data.data)
            ResourceContentType.RelativePath -> error("Relative path can't be loaded")
            ResourceContentType.Uri -> data.data.toUri()
        }
    }
}
