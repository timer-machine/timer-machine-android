package xyz.aprildown.timer.data.mappers

import dagger.Reusable
import xyz.aprildown.timer.data.datas.TimerStampData
import xyz.aprildown.timer.domain.Mapper
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import javax.inject.Inject

@Reusable
internal class TimerStampMapper @Inject constructor() : Mapper<TimerStampData, TimerStampEntity>() {
    override fun mapFrom(from: TimerStampData): TimerStampEntity {
        return TimerStampEntity(
            id = from.id,
            timerId = from.timerId,
            start = if (from.start == 0L) from.end else from.start,
            end = from.end
        )
    }

    override fun mapTo(from: TimerStampEntity): TimerStampData {
        return TimerStampData(
            id = from.id,
            timerId = from.timerId,
            start = from.start,
            end = from.end
        )
    }
}
