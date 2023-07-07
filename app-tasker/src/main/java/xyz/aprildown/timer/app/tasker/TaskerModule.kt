package xyz.aprildown.timer.app.tasker

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.aprildown.timer.domain.repositories.TaskerEventTrigger

@InstallIn(SingletonComponent::class)
@Module
internal abstract class TaskerModule {
    @Binds
    abstract fun bindTaskerEventTrigger(impl: TaskerEventTriggerImpl): TaskerEventTrigger
}
