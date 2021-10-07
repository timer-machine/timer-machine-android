package xyz.aprildown.timer.domain.repositories

import xyz.aprildown.timer.domain.entities.SchedulerEntity

interface SchedulerRepository {
    suspend fun items(): List<SchedulerEntity>
    suspend fun item(id: Int): SchedulerEntity?
    suspend fun add(item: SchedulerEntity): Int
    suspend fun save(item: SchedulerEntity): Boolean
    suspend fun delete(id: Int)
    suspend fun setSchedulerEnable(id: Int, enable: Int)
    suspend fun schedulersWithTimerId(id: Int): List<SchedulerEntity>
}
