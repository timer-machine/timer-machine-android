package xyz.aprildown.timer.domain.entities

enum class StepType {
    NORMAL, NOTIFIER, START, END
}

sealed class StepEntity {
    data class Step(
        val label: String,
        val length: Long,
        val behaviour: List<BehaviourEntity> = emptyList(),
        val type: StepType = StepType.NORMAL
    ) : StepEntity()

    data class Group(
        val name: String,
        val loop: Int,
        val steps: List<StepEntity>
    ) : StepEntity()
}
