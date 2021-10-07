package xyz.aprildown.timer.data.mappers

import dagger.Reusable
import xyz.aprildown.timer.data.datas.SchedulerData
import xyz.aprildown.timer.domain.Mapper
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import javax.inject.Inject

@Reusable
internal class SchedulerMapper @Inject constructor() : Mapper<SchedulerData, SchedulerEntity>() {
    override fun mapFrom(from: SchedulerData): SchedulerEntity {
        return SchedulerEntity(
            id = from.id,
            timerId = from.timerId,
            label = from.label,
            action = from.action,
            hour = from.hour,
            minute = from.minute,
            repeatMode = from.repeatMode!!,
            days = from.days,
            enable = from.enable
        )
    }

    override fun mapTo(from: SchedulerEntity): SchedulerData {
        return SchedulerData(
            id = from.id,
            timerId = from.timerId,
            label = from.label,
            action = from.action,
            hour = from.hour,
            minute = from.minute,
            repeatMode = from.repeatMode,
            days = from.days,
            enable = from.enable
        )
    }
}
