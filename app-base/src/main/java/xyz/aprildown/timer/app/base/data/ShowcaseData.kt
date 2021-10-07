package xyz.aprildown.timer.app.base.data

import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.StepType

object ShowcaseData {
    fun getSampleSteps(): List<StepEntity.Step> = listOf(
        StepEntity.Step("Run", 180_000, listOf()),
        StepEntity.Step(
            "Rest", 5_000, listOf(
                BehaviourEntity(BehaviourType.MUSIC),
                BehaviourEntity(BehaviourType.VOICE)
            ),
            StepType.NOTIFIER
        ),
        StepEntity.Step("Walk", 60_000, listOf()),
        StepEntity.Step(
            "Rest", 5_000, listOf(
                BehaviourEntity(BehaviourType.MUSIC),
                BehaviourEntity(BehaviourType.VIBRATION),
                BehaviourEntity(BehaviourType.SCREEN)
            ),
            StepType.NOTIFIER
        )
    )
}
