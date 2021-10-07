package xyz.aprildown.timer.data.repositories

import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import xyz.aprildown.timer.data.db.TimerDao
import xyz.aprildown.timer.data.mappers.TimerInfoMapper
import xyz.aprildown.timer.data.mappers.TimerMapper
import xyz.aprildown.timer.data.mappers.fromWithMapper
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.repositories.TimerRepository
import javax.inject.Inject

@Reusable
internal class TimerRepositoryImpl @Inject constructor(
    private val timerDao: TimerDao,
    private val timerMapper: TimerMapper,
    private val timerInfoMapper: TimerInfoMapper
) : TimerRepository {

    override suspend fun items(): List<TimerEntity> {
        return timerDao.getTimers().map { timerMapper.mapFrom(it) }
    }

    override suspend fun item(id: Int): TimerEntity? {
        return timerDao.getTimer(id).let {
            return@let if (it == null) null else timerMapper.mapFrom(it)
        }
    }

    override suspend fun add(item: TimerEntity): Int {
        return timerDao.addTimer(timerMapper.mapTo(item)).toInt()
    }

    override suspend fun save(item: TimerEntity): Boolean {
        return timerDao.updateTimer(timerMapper.mapTo(item)) == 1
    }

    override suspend fun delete(id: Int) {
        timerDao.deleteTimer(id)
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
