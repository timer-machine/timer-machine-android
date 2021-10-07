package xyz.aprildown.timer.data.repositories

import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import dagger.Reusable
import xyz.aprildown.timer.data.db.TimerStampDao
import xyz.aprildown.timer.data.mappers.TimerStampMapper
import xyz.aprildown.timer.data.mappers.fromWithMapper
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import javax.inject.Inject

@Reusable
internal class TimerStampRepositoryImpl @Inject constructor(
    private val dao: TimerStampDao,
    private val mapper: TimerStampMapper
) : TimerStampRepository {

    override suspend fun getAll(): List<TimerStampEntity> {
        return mapper.mapFrom(dao.getTimerStamps())
    }

    override suspend fun getRecordsGroupedByTimer(
        timerIds: List<Int>,
        startTime: Long,
        endTime: Long
    ): ArrayMap<Int, List<TimerStampEntity>> {
        val result = arrayMapOf<Int, List<TimerStampEntity>>()

        timerIds.forEach { timerId ->
            result[timerId] = dao.getWithTimerIdAndSpan(timerId, startTime, endTime)
                .fromWithMapper(mapper)
        }

        return result
    }

    override suspend fun getRaw(
        timerIds: List<Int>,
        startTime: Long,
        endTime: Long
    ): List<TimerStampEntity> {
        val result = mutableListOf<TimerStampEntity>()

        timerIds.forEach { timerId ->
            result += dao.getWithTimerIdAndSpan(timerId, startTime, endTime)
                .fromWithMapper(mapper)
        }

        return result
    }

    override suspend fun getRecentOne(timerId: Int): TimerStampEntity? {
        return dao.getRecentOne(timerId)?.fromWithMapper(mapper)
    }

    override suspend fun getEarliestOne(): TimerStampEntity? {
        return dao.getEarliestOne()?.fromWithMapper(mapper)
    }

    override suspend fun add(stamp: TimerStampEntity): Int {
        return dao.add(mapper.mapTo(stamp)).toInt()
    }

    override suspend fun deleteWithTimerId(timerId: Int): Int {
        return dao.deleteWithTimerId(timerId)
    }

    override suspend fun delete(id: Int) {
        dao.deleteTimerStamp(id)
    }
}
