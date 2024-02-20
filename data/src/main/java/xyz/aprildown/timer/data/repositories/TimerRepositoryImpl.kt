package xyz.aprildown.timer.data.repositories

import android.content.ContentResolver
import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.aprildown.timer.data.db.TimerDao
import xyz.aprildown.timer.data.mappers.TimerInfoMapper
import xyz.aprildown.timer.data.mappers.TimerMapper
import xyz.aprildown.timer.data.mappers.fromWithMapper
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.ImageAction
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.toImageAction
import xyz.aprildown.timer.domain.repositories.TimerRepository
import java.io.File
import java.util.UUID
import javax.inject.Inject

@Reusable
internal class TimerRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val timerDao: TimerDao,
    private val timerMapper: TimerMapper,
    private val timerInfoMapper: TimerInfoMapper,
) : TimerRepository {
    private val imageSaver: TimerImageSaver by lazy { TimerImageSaver(context) }

    override suspend fun items(): List<TimerEntity> {
        return timerDao.getTimers().map { timerMapper.mapFrom(it).withRealImagePath() }
    }

    override suspend fun item(id: Int): TimerEntity? {
        return timerDao.getTimer(id).let {
            return@let if (it == null) null else timerMapper.mapFrom(it).withRealImagePath()
        }
    }

    private fun TimerEntity.withRealImagePath(): TimerEntity {
        return transformImageActions {
            it.copy(path = imageSaver.getFullPath(id, it.path))
        }
    }

    override suspend fun add(item: TimerEntity): Int {
        val tempTimerFolder = imageSaver.getTimerFolder(item.id)
        val timerId = timerDao.addTimer(timerMapper.mapTo(item.saveImages())).toInt()
        if (tempTimerFolder.exists()) {
            val newTimerFolder = imageSaver.getTimerFolder(timerId)
            tempTimerFolder.copyRecursively(newTimerFolder)
            tempTimerFolder.deleteRecursively()
        }
        return timerId
    }

    override suspend fun save(item: TimerEntity): Boolean {
        try {
            imageSaver.trackSavedImages()
            return timerDao.updateTimer(timerMapper.mapTo(item.saveImages())) == 1
        } finally {
            imageSaver.deleteUnusedImages(item.id)
        }
    }

    private fun TimerEntity.saveImages(): TimerEntity {
        return transformImageActions {
            it.copy(path = imageSaver.save(id, it.path))
        }
    }

    private fun TimerEntity.transformImageActions(
        transform: (ImageAction) -> ImageAction
    ): TimerEntity {
        fun StepEntity.transformStep(): StepEntity {
            return when (this) {
                is StepEntity.Step -> {
                    if (behaviour.any { it.type == BehaviourType.IMAGE }) {
                        copy(
                            behaviour = behaviour.map {
                                if (it.type == BehaviourType.IMAGE) {
                                    transform(it.toImageAction()).toBehaviourEntity()
                                } else {
                                    it
                                }
                            }
                        )
                    } else {
                        this
                    }
                }
                is StepEntity.Group -> {
                    copy(steps = steps.map { it.transformStep() })
                }
            }
        }
        return copy(
            steps = steps.map { it.transformStep() },
            startStep = startStep?.transformStep() as? StepEntity.Step,
            endStep = endStep?.transformStep() as? StepEntity.Step,
        )
    }

    override suspend fun delete(id: Int) {
        timerDao.deleteTimer(id)
        imageSaver.deleteAll(id)
    }

    override suspend fun getTimerInfoByTimerId(timerId: Int): TimerInfo? {
        return timerDao.findTimerInfo(timerId)?.fromWithMapper(timerInfoMapper)
    }

    override suspend fun getTimerInfo(folderId: Long): List<TimerInfo> {
        return timerDao.getTimerInfo(folderId).fromWithMapper(timerInfoMapper)
    }

    override fun getTimerInfoFlow(folderId: Long): Flow<List<TimerInfo>> {
        return timerDao.getTimerInfoFlow(folderId).map { it.fromWithMapper(timerInfoMapper) }
    }

    override suspend fun changeTimerFolder(timerId: Int, folderId: Long) {
        timerDao.changeTimerFolder(timerId = timerId, folderId = folderId)
    }

    override suspend fun moveFolderTimersToAnother(originalFolderId: Long, targetFolderId: Long) {
        timerDao.moveFolderTimersToAnother(originalFolderId, targetFolderId)
    }
}

private class TimerImageSaver(private val context: Context) {
    private val imageFolder = File(context.filesDir, FOLDER)

    private var savedFilenames = mutableListOf<String>()

    fun getTimerFolder(timerId: Int): File {
        return File(imageFolder, timerId.toString())
    }

    fun getFullPath(timerId: Int, path: String): String {
        return File(getTimerFolder(timerId), path).toUri().toString()
    }

    /**
     * @return The saved image filename
     */
    fun save(timerId: Int, path: String): String {
        val uri = path.toUri()
        val timerFolder = getTimerFolder(timerId)
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            val imageFile = uri.toFile()
            if (imageFile.parentFile?.canonicalPath == timerFolder.canonicalPath) {
                return imageFile.name.also {
                    savedFilenames += it
                }
            }
        }

        if (!imageFolder.exists()) imageFolder.mkdirs()
        if (!timerFolder.exists()) timerFolder.mkdirs()

        return when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "jpg"
                val imageFile = File(timerFolder, "${UUID.randomUUID()}.$extension")
                if (imageFile.exists()) {
                    imageFile.delete()
                    imageFile.createNewFile()
                }
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    imageFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                require(imageFile.exists() && imageFile.length() > 0L)
                imageFile.name.also {
                    savedFilenames += it
                }
            }
            else -> error("Unsupported path $path")
        }
    }

    fun trackSavedImages() {
        savedFilenames.clear()
    }

    fun deleteUnusedImages(timerId: Int) {
        val timerFolder = getTimerFolder(timerId)
        if (!timerFolder.exists()) return
        timerFolder.listFiles()?.toList()?.forEach { file ->
            if (file.name !in savedFilenames) {
                file.delete()
            }
        }
        savedFilenames.clear()
    }

    fun deleteAll(timerId: Int) {
        val timerFolder = getTimerFolder(timerId)
        if (timerFolder.exists()) {
            timerFolder.deleteRecursively()
        }
    }

    companion object {
        private const val FOLDER = "timer_images"
    }
}
