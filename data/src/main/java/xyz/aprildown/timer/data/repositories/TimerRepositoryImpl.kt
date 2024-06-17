package xyz.aprildown.timer.data.repositories

import android.content.ContentResolver
import android.content.Context
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
import xyz.aprildown.timer.domain.entities.ImageAction
import xyz.aprildown.timer.domain.entities.ResourceContentType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.inferResourcesContentType
import xyz.aprildown.timer.domain.repositories.TimerRepository
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
        return timerDao.getTimers().map {
            timerMapper.mapFrom(it).withImageCanonicalPath()
        }
    }

    override suspend fun item(id: Int): TimerEntity? {
        return timerDao.getTimer(id).let {
            return@let if (it == null) null else timerMapper.mapFrom(it).withImageCanonicalPath()
        }
    }

    private fun TimerEntity.withImageCanonicalPath(): TimerEntity {
        return this
        // return transformImageActions { data ->
        //     ImageAction(
        //         data = data.convertImageData(
        //             id = id,
        //             imageManager = imageManager,
        //             targetType = ResourceContentType.CanonicalPath,
        //         ),
        //     )
        // }
    }

    override suspend fun add(item: TimerEntity): Int {
        val tempTimerFolder = imageManager.getTimerFolder(item.id)
        val timerId = timerDao.addTimer(
            timerMapper.mapTo(
                item.transformImageActions {
                    ImageAction(
                        data = imageManager
                            .save(id = item.id, data = it)
                            .convertImageData(
                                id = item.id,
                                imageManager = imageManager,
                                targetType = ResourceContentType.RelativePath,
                            ),
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
        val savedItem = imageManager.trackSaving(id = item.id) {
            item.transformImageActions {
                ImageAction(
                    data = save(data = it).convertImageData(
                        id = item.id,
                        imageManager = imageManager,
                        targetType = ResourceContentType.RelativePath,
                    ),
                )
            }
        }
        return timerDao.updateTimer(timerMapper.mapTo(savedItem)) == 1
    }

    private fun TimerEntity.transformImageActions(
        transform: (data: String) -> ImageAction
    ): TimerEntity {
        return this
        // fun StepEntity.transformStep(): StepEntity {
        //     return when (this) {
        //         is StepEntity.Step -> {
        //             if (behaviour.any { it.type == BehaviourType.IMAGE }) {
        //                 copy(
        //                     behaviour = behaviour.map {
        //                         if (it.type == BehaviourType.IMAGE) {
        //                             transform(it.toImageAction().data).toBehaviourEntity()
        //                         } else {
        //                             it
        //                         }
        //                     }
        //                 )
        //             } else {
        //                 this
        //             }
        //         }
        //         is StepEntity.Group -> {
        //             copy(steps = steps.map { it.transformStep() })
        //         }
        //     }
        // }
        // return copy(
        //     steps = steps.map { it.transformStep() },
        //     startStep = startStep?.transformStep() as? StepEntity.Step,
        //     endStep = endStep?.transformStep() as? StepEntity.Step,
        // )
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
                    data = data.convertImageData(
                        id = timer.id,
                        imageManager = imageManager,
                        targetType = type,
                    )
                )
            }
        }
    }
}

private class TimerImageManager(context: Context) {
    interface TrackingScope {
        /**
         * [TimerImageManager.save]
         */
        fun save(data: String): String
    }

    val contentResolver: ContentResolver = context.contentResolver

    private val imageFolder = File(context.filesDir, FOLDER)

    fun getTimerFolder(timerId: Int): File {
        return File(imageFolder, timerId.toString())
    }

    /**
     * @return [ResourceContentType.RelativePath], [File.getCanonicalPath]
     */
    fun save(id: Int, data: String): String {
        imageFolder.ensureDirExistence()

        val timerFolder = getTimerFolder(id)
        val imageFile: File
        when (data.inferResourcesContentType()) {
            ResourceContentType.CanonicalPath -> {
                val existingFile = File(data)
                if (existingFile.parentFile?.canonicalPath == timerFolder.canonicalPath) {
                    imageFile = existingFile
                } else {
                    imageFile = File(
                        timerFolder.ensureDirExistence(),
                        "${generateImageName()}.${existingFile.extension}"
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
                    .getExtensionFromMimeType(contentResolver.getType(uri)) ?: "jpg"
                imageFile = File(
                    timerFolder.ensureDirExistence(),
                    "${generateImageName()}.$extension"
                ).ensureNewFile()
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    imageFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                imageFile.canonicalPath
            }
        }
        require(imageFile.exists() && imageFile.length() > 0L)
        return imageFile.canonicalPath
    }

    private fun generateImageName(): String {
        return UUID.randomUUID().toString()
    }

    fun <T> trackSaving(id: Int, block: TrackingScope.() -> T): T {
        val saved = mutableListOf<String>()
        val scope = object : TrackingScope {
            override fun save(data: String): String {
                val savedData = save(id = id, data = data)
                saved += savedData
                return savedData
            }
        }
        return try {
            block(scope)
        } finally {
            val timerFolder = getTimerFolder(id)
            if (timerFolder.exists()) {
                timerFolder.listFiles()?.toList()?.forEach { file ->
                    if (file.canonicalPath !in saved) {
                        file.delete()
                    }
                }
            }
        }
    }

    fun delete(id: Int) {
        val timerFolder = getTimerFolder(id)
        if (timerFolder.exists()) {
            timerFolder.deleteRecursively()
        }
    }

    companion object {
        private const val FOLDER = "timer_images"
    }
}

private fun String.convertImageData(
    id: Int = TimerEntity.NULL_ID,
    imageManager: TimerImageManager,
    targetType: ResourceContentType,
): String {
    fun String.save(): String {
        return imageManager.save(id = id, data = this)
    }

    fun String.convert(targetType: ResourceContentType): String {
        return convertImageData(id = id, imageManager = imageManager, targetType)
    }

    return when (inferResourcesContentType()) {
        ResourceContentType.CanonicalPath -> {
            when (targetType) {
                ResourceContentType.CanonicalPath -> save()
                ResourceContentType.RelativePath -> File(this).name
                ResourceContentType.Uri -> error("Unable to convert CanonicalPath to Uri")
            }
        }
        ResourceContentType.RelativePath -> {
            when (targetType) {
                ResourceContentType.CanonicalPath -> save()
                ResourceContentType.RelativePath -> return this
                ResourceContentType.Uri -> error("Unable to convert RelativePath to Uri")
            }
        }
        ResourceContentType.Uri -> {
            when (targetType) {
                ResourceContentType.CanonicalPath -> save()
                ResourceContentType.RelativePath -> save().convert(ResourceContentType.RelativePath)
                ResourceContentType.Uri -> return this
            }
        }
    }
}
