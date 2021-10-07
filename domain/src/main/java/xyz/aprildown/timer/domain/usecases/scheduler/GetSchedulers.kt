package xyz.aprildown.timer.domain.usecases.scheduler

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class GetSchedulers @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SchedulerRepository
) : CoroutinesUseCase<Unit, List<SchedulerEntity>>(dispatcher) {
    override suspend fun create(params: Unit): List<SchedulerEntity> {
        return repository.items()
    }
}
