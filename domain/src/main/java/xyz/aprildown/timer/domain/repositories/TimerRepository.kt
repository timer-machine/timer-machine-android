package xyz.aprildown.timer.domain.repositories

import kotlinx.coroutines.flow.Flow
import xyz.aprildown.timer.domain.entities.ResourceContentType
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo

interface TimerRepository {
    suspend fun items(): List<TimerEntity>
    suspend fun item(id: Int): TimerEntity?
    suspend fun add(item: TimerEntity): Int
    suspend fun save(item: TimerEntity): Boolean
    suspend fun delete(id: Int)
    suspend fun getTimerInfoByTimerId(timerId: Int): TimerInfo?
    fun getTimerInfoFlow(folderId: Long): Flow<List<TimerInfo>>
    suspend fun getTimerInfo(folderId: Long): List<TimerInfo>
    suspend fun changeTimerFolder(timerId: Int, folderId: Long)
    suspend fun moveFolderTimersToAnother(originalFolderId: Long, targetFolderId: Long)
    suspend fun changeContentType(
        timers: List<TimerEntity>,
        type: ResourceContentType,
    ): List<TimerEntity>
}
