package xyz.aprildown.timer.app.broadcast

import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.entities.TimerEntity

internal object TimerFactory {

    fun createCountdownTimer(durationSeconds: Long, name: String): TimerEntity {
        val durationMillis = durationSeconds * 1000L

        return TimerEntity(
            id = TimerEntity.NEW_ID,
            name = name,
            loop = 1,
            steps = listOf(
                StepEntity.Step(
                    label = name,
                    length = durationMillis
                ),
                StepEntity.Step(
                    label = name,
                    length = 10_000L,
                    behaviour = listOf(
                        BehaviourEntity(type = BehaviourType.MUSIC),
                        BehaviourEntity(type = BehaviourType.VIBRATION),
                        BehaviourEntity(type = BehaviourType.SCREEN),
                        BehaviourEntity(type = BehaviourType.HALT),
                    ),
                    type = StepType.NOTIFIER
                )
            )
        )
    }
}
