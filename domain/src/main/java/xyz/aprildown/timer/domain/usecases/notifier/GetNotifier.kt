package xyz.aprildown.timer.domain.usecases.notifier

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class GetNotifier @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: NotifierRepository
) : CoroutinesUseCase<Unit, StepEntity.Step>(dispatcher) {
    override suspend fun create(params: Unit): StepEntity.Step {
        return repository.get()
    }
}
