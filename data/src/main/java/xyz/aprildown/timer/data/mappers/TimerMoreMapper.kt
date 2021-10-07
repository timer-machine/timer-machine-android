package xyz.aprildown.timer.data.mappers

import dagger.Reusable
import xyz.aprildown.timer.data.datas.TimerMoreData
import xyz.aprildown.timer.domain.Mapper
import xyz.aprildown.timer.domain.entities.TimerMoreEntity
import javax.inject.Inject

@Reusable
internal class TimerMoreMapper @Inject constructor() : Mapper<TimerMoreData, TimerMoreEntity>() {

    override fun mapFrom(from: TimerMoreData): TimerMoreEntity {
        return TimerMoreEntity(
            showNotif = from.showNotif,
            notifCount = from.notifCount,
            triggerTimerId = from.triggerTimerId
        )
    }

    override fun mapTo(from: TimerMoreEntity): TimerMoreData {
        return TimerMoreData(
            showNotif = from.showNotif,
            notifCount = from.notifCount,
            triggerTimerId = from.triggerTimerId
        )
    }
}
