package xyz.aprildown.timer.domain.repositories

import xyz.aprildown.timer.domain.entities.TimerEntity

interface SampleTimerRepository {
    suspend fun getSampleTimer(id: Int): TimerEntity
}
