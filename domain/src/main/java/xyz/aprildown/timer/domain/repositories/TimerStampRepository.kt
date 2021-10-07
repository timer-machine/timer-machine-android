package xyz.aprildown.timer.domain.repositories

import androidx.collection.ArrayMap
import xyz.aprildown.timer.domain.entities.TimerStampEntity

interface TimerStampRepository {

    suspend fun getAll(): List<TimerStampEntity>

    suspend fun getRecordsGroupedByTimer(
        timerIds: List<Int>,
        startTime: Long,
        endTime: Long
    ): ArrayMap<Int, List<TimerStampEntity>>

    suspend fun getRaw(timerIds: List<Int>, startTime: Long, endTime: Long): List<TimerStampEntity>

    suspend fun getRecentOne(timerId: Int): TimerStampEntity?

    suspend fun getEarliestOne(): TimerStampEntity?

    /**
     * @return The added entity's id
     */
    suspend fun add(stamp: TimerStampEntity): Int

    /**
     * @return Delete count
     */
    suspend fun deleteWithTimerId(timerId: Int): Int

    suspend fun delete(id: Int)
}
