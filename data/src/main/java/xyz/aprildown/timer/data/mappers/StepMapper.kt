package xyz.aprildown.timer.data.mappers

import dagger.Reusable
import xyz.aprildown.timer.data.datas.StepData
import xyz.aprildown.timer.domain.Mapper
import xyz.aprildown.timer.domain.entities.StepEntity
import javax.inject.Inject

@Reusable
internal class StepMapper @Inject constructor(
    val stepOnlyMapper: StepOnlyMapper
) : Mapper<StepData, StepEntity>() {
    override fun mapFrom(from: StepData): StepEntity {
        return when (from) {
            is StepData.Step -> stepOnlyMapper.mapFrom(from)
            is StepData.Group -> StepEntity.Group(
                name = from.name,
                loop = from.loop,
                steps = mapFrom(from.steps)
            )
        }
    }

    override fun mapTo(from: StepEntity): StepData {
        return when (from) {
            is StepEntity.Step -> stepOnlyMapper.mapTo(from)
            is StepEntity.Group -> StepData.Group(
                name = from.name,
                loop = from.loop,
                steps = mapTo(from.steps)
            )
        }
    }
}

@Reusable
internal class StepOnlyMapper @Inject constructor(
    private val behaviourMapper: BehaviourMapper
) : Mapper<StepData.Step, StepEntity.Step>() {
    override fun mapFrom(from: StepData.Step): StepEntity.Step {
        return StepEntity.Step(
            label = from.label,
            length = from.length,
            behaviour = behaviourMapper.mapFrom(from.behaviour),
            type = from.type
        )
    }

    override fun mapTo(from: StepEntity.Step): StepData.Step {
        return StepData.Step(
            label = from.label,
            length = from.length,
            behaviour = behaviourMapper.mapTo(from.behaviour),
            type = from.type
        )
    }
}
