package xyz.aprildown.timer.data.repositories

import android.content.Context
import android.graphics.BitmapFactory
import android.webkit.MimeTypeMap
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
import xyz.aprildown.timer.domain.entities.ResourceContentType
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.inferResourcesContentType
import xyz.aprildown.timer.domain.entities.toImageAction
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.utils.Base64BitmapConverter.decodeBitmapByteArrayFromBase64
import xyz.aprildown.timer.domain.utils.Base64BitmapConverter.encodeToBase64
import xyz.aprildown.timer.domain.utils.ensureDirExistence
import xyz.aprildown.timer.domain.utils.ensureNewFile
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
    private val imageManager by lazy { TimerImageManager(context) }

    override suspend fun items(): List<TimerEntity> {
        return timerDao.getTimers().map { timerMapper.mapFrom(it).withImageCanonicalPath() }
    }

    override suspend fun item(id: Int): TimerEntity? {
        return timerDao.getTimer(id).let {
            return@let if (it == null) null else timerMapper.mapFrom(it).withImageCanonicalPath()
        }
    }

    private fun TimerEntity.withImageCanonicalPath(): TimerEntity {
        return transformImageActions { data ->
            ImageAction(data = data.convertImageDataToCanonicalPath(id))
        }
    }

    override suspend fun add(item: TimerEntity): Int {
        val tempTimerFolder = imageManager.getTimerFolder(item.id)
        val timerId = timerDao.addTimer(
            timerMapper.mapTo(
                item.transformImageActions {
                    ImageAction(
                        data = imageManager.save(timerId = item.id, data = it)
                            .convertImageCanonicalPathToDataType(ResourceContentType.RelativePath)
                    )
                }
            )
        ).toInt()
        if (tempTimerFolder.exists()) {
            val newTimerFolder = imageManager.getTimerFolder(timerId)
            tempTimerFolder.copyRecursively(newTimerFolder)
            tempTimerFolder.deleteRecursively()
        }
        return timerId
    }

    override suspend fun save(item: TimerEntity): Boolean {
        val savedItem = imageManager.trackSaving(timerId = item.id) {
            item.transformImageActions {
                ImageAction(
                    data = save(data = it)
                        .convertImageCanonicalPathToDataType(ResourceContentType.RelativePath)
                )
            }
        }
        return timerDao.updateTimer(timerMapper.mapTo(savedItem)) == 1
    }

    private fun TimerEntity.transformImageActions(
        transform: (data: String) -> ImageAction
    ): TimerEntity {
        fun StepEntity.transformStep(): StepEntity {
            return when (this) {
                is StepEntity.Step -> {
                    if (behaviour.any { it.type == BehaviourType.IMAGE }) {
                        copy(
                            behaviour = behaviour.map {
                                if (it.type == BehaviourType.IMAGE) {
                                    transform(it.toImageAction().data).toBehaviourEntity()
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
        imageManager.delete(id)
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

    override suspend fun changeContentType(
        timers: List<TimerEntity>,
        type: ResourceContentType,
    ): List<TimerEntity> {
        return timers.map { timer ->
            timer.transformImageActions { data ->
                ImageAction(
                    data = data.convertImageDataToCanonicalPath(timerId = timer.id)
                        .convertImageCanonicalPathToDataType(type = type),
                )
            }
        }
    }

    private fun String.convertImageDataToCanonicalPath(timerId: Int): String {
        return imageManager.save(timerId = timerId, data = this)
    }

    private fun String.convertImageCanonicalPathToDataType(type: ResourceContentType): String {
        return when (type) {
            ResourceContentType.CanonicalPath -> this
            ResourceContentType.RelativePath -> File(this).name
            ResourceContentType.Uri -> error("Unsupported")
            ResourceContentType.Base64 -> {
                BitmapFactory.decodeFile(this).encodeToBase64(quality = 90)
            }
        }
    }
}

private class TimerImageManager(private val context: Context) {
    interface TrackingScope {
        /**
         * [TimerImageManager.save]
         */
        fun save(data: String): String
    }

    private val imageFolder = File(context.filesDir, FOLDER)

    fun getTimerFolder(timerId: Int): File {
        return File(imageFolder, timerId.toString())
    }

    /**
     * @return [ResourceContentType.RelativePath], [File.getCanonicalPath]
     */
    fun save(timerId: Int, data: String): String {
        imageFolder.ensureDirExistence()

        val timerFolder = getTimerFolder(timerId)
        val imageFile: File
        when (data.inferResourcesContentType()) {
            ResourceContentType.CanonicalPath -> {
                val existingFile = File(data)
                if (existingFile.parentFile?.canonicalPath == timerFolder.canonicalPath) {
                    imageFile = existingFile
                } else {
                    imageFile = File(
                        imageFolder.ensureDirExistence(),
                        "${UUID.randomUUID()}.${existingFile.extension}"
                    ).ensureNewFile()
                    imageFile.inputStream().use { inputStream ->
                        existingFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
            ResourceContentType.RelativePath -> {
                imageFile = File(timerFolder.ensureDirExistence(), data)
            }
            ResourceContentType.Uri -> {
                val uri = data.toUri()
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "jpg"
                imageFile =
                    File(timerFolder.ensureDirExistence(), "${UUID.randomUUID()}.$extension")
                        .ensureNewFile()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    imageFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                require(imageFile.exists() && imageFile.length() > 0L)
                imageFile.canonicalPath
            }
            ResourceContentType.Base64 -> {
                imageFile = File(timerFolder.ensureDirExistence(), "${UUID.randomUUID()}.webp")
                    .ensureNewFile()
                val byteArray = data.decodeBitmapByteArrayFromBase64()
                imageFile.outputStream().use {
                    it.write(byteArray)
                    it.flush()
                }
            }
        }
        require(imageFile.exists() && imageFile.length() > 0L)
        return imageFile.canonicalPath
    }

    fun <T> trackSaving(timerId: Int, block: TrackingScope.() -> T): T {
        val saved = mutableListOf<String>()
        val scope = object : TrackingScope {
            override fun save(data: String): String {
                val savedData = save(timerId = timerId, data = data)
                saved += savedData
                return savedData
            }
        }
        return try {
            block(scope)
        } finally {
            val timerFolder = getTimerFolder(timerId)
            if (timerFolder.exists()) {
                timerFolder.listFiles()?.toList()?.forEach { file ->
                    if (file.canonicalPath !in saved) {
                        file.delete()
                    }
                }
            }
        }
    }

    fun delete(timerId: Int) {
        val timerFolder = getTimerFolder(timerId)
        if (timerFolder.exists()) {
            timerFolder.deleteRecursively()
        }
    }

    companion object {
        private const val FOLDER = "timer_images"
    }
}
