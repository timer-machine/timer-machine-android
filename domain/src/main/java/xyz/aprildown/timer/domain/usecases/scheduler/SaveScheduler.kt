package xyz.aprildown.timer.domain.usecases.scheduler

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.SchedulerExecutor
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class SaveScheduler @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SchedulerRepository,
    private val executor: SchedulerExecutor,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<SchedulerEntity, Boolean>(dispatcher) {
    override suspend fun create(params: SchedulerEntity): Boolean {
        if (params.isNull) return false

        // Cancel the old scheduler before updating it
        repository.item(params.id)?.let { executor.cancel(it) }
        return repository.save(params).also {
            appDataRepository.notifyDataChanged()
        }
    }
}
