package xyz.aprildown.timer.data.repositories

import dagger.Reusable
import xyz.aprildown.timer.data.db.SchedulerDao
import xyz.aprildown.timer.data.mappers.SchedulerMapper
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import javax.inject.Inject

@Reusable
internal class SchedulerRepositoryImpl @Inject constructor(
    private val schedulerDao: SchedulerDao,
    private val schedulerMapper: SchedulerMapper
) : SchedulerRepository {

    override suspend fun items(): List<SchedulerEntity> {
        return schedulerDao.getSchedulers().map { schedulerMapper.mapFrom(it) }
    }

    override suspend fun item(id: Int): SchedulerEntity? {
        return schedulerDao.getScheduler(id).let {
            return@let if (it == null) null else schedulerMapper.mapFrom(it)
        }
    }

    override suspend fun add(item: SchedulerEntity): Int {
        return schedulerDao.addScheduler(schedulerMapper.mapTo(item)).toInt()
    }

    override suspend fun save(item: SchedulerEntity): Boolean {
        return schedulerDao.updateScheduler(schedulerMapper.mapTo(item)) == 1
    }

    override suspend fun delete(id: Int) {
        schedulerDao.deleteScheduler(id)
    }

    override suspend fun setSchedulerEnable(id: Int, enable: Int) {
        schedulerDao.setSchedulerEnable(id, enable)
    }

    override suspend fun schedulersWithTimerId(id: Int): List<SchedulerEntity> {
        return schedulerDao.getSchedulersByTimerId(id).map { schedulerMapper.mapFrom(it) }
    }
}
