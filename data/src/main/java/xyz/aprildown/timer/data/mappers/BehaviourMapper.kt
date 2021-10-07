package xyz.aprildown.timer.data.mappers

import dagger.Reusable
import xyz.aprildown.timer.data.datas.BehaviourData
import xyz.aprildown.timer.domain.Mapper
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import javax.inject.Inject

@Reusable
internal class BehaviourMapper @Inject constructor() : Mapper<BehaviourData, BehaviourEntity>() {
    override fun mapFrom(from: BehaviourData): BehaviourEntity {
        return BehaviourEntity(
            type = from.type,
            str1 = from.label,
            str2 = from.content,
            bool = from.loop
        )
    }

    override fun mapTo(from: BehaviourEntity): BehaviourData {
        return BehaviourData(
            type = from.type,
            label = from.str1,
            content = from.str2,
            loop = from.bool
        )
    }
}
