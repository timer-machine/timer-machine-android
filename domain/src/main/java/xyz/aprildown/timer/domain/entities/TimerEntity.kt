package xyz.aprildown.timer.domain.entities

import android.content.ContentResolver
import okio.Path
import xyz.aprildown.timer.domain.utils.Base64BitmapConverter

/**
 * Whenever you add a new more item, you need to check
 * TestData, TimerMoreMapper, MappersTest, OneFragment, EditActivity
 */
data class TimerMoreEntity(
    val showNotif: Boolean = true,
    val notifCount: Boolean = true,
    val triggerTimerId: Int = TimerEntity.NULL_ID
)

data class TimerEntity(
    val id: Int,
    val name: String,
    val loop: Int,
    val steps: List<StepEntity>,
    val startStep: StepEntity.Step? = null,
    val endStep: StepEntity.Step? = null,
    val more: TimerMoreEntity = TimerMoreEntity(),
    val folderId: Long = FolderEntity.FOLDER_DEFAULT
) {
    val isNull get() = id == NULL_ID

    companion object {
        const val NEW_ID = 0
        const val NULL_ID = 0
    }
}

enum class ResourceContentType {
    CanonicalPath,
    RelativePath,
    Uri,
    Base64;
}

fun String.inferResourcesContentType(): ResourceContentType {
    return when {
        startsWith(Path.DIRECTORY_SEPARATOR) -> ResourceContentType.CanonicalPath
        startsWith(ContentResolver.SCHEME_CONTENT) -> ResourceContentType.Uri
        startsWith(Base64BitmapConverter.PREFIX) -> ResourceContentType.Base64
        else -> ResourceContentType.RelativePath
    }
}

/**
 * For query and display. Loading [TimerEntity] is too heavy.
 */
data class TimerInfo(
    val id: Int,
    val name: String,
    val folderId: Long = FolderEntity.FOLDER_DEFAULT
)

fun TimerEntity.toTimerInfo(): TimerInfo {
    return TimerInfo(
        id = id,
        name = name,
        folderId = folderId
    )
}
