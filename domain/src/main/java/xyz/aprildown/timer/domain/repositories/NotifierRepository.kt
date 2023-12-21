package xyz.aprildown.timer.domain.repositories

import xyz.aprildown.timer.domain.entities.StepEntity

interface NotifierRepository {
    suspend fun get(): StepEntity.Step
    suspend fun set(item: StepEntity.Step?): Boolean

    companion object {
        const val NAMED_DEFAULT_NOTIFIER_NAME = "default_notifier_name"
    }
}
